package com.innowise.userservice.service;

import com.innowise.userservice.exception.EmailAlreadyExistsException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.integration.keycloak.KeycloakAdminClient;
import com.innowise.userservice.model.dto.UserCreateDto;
import com.innowise.userservice.model.dto.UserResponseDto;
import com.innowise.userservice.model.dto.UserUpdateDto;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.repository.spec.UserSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.UUID;

/**
 * Keycloak is an external system, so no HTTP call to it happens inside a database
 * transaction: local writes run in short transactional blocks (TransactionTemplate),
 * and the Keycloak call is made outside of them. If Keycloak fails after a local
 * write, the local change is compensated back; if the local write fails after a
 * Keycloak change, the Keycloak side is compensated and any compensation failure
 * is logged at ERROR so ghost accounts do not go unnoticed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String USER_NOT_FOUND = "User not found: ";

    private final UserRepository userRepository;
    private final PaymentCardRepository paymentCardRepository;
    private final UserMapper userMapper;
    private final KeycloakAdminClient keycloakAdminClient;
    private final TransactionTemplate transactionTemplate;

    public UserResponseDto create(UserCreateDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException("Email already in use: " + dto.getEmail());
        }

        UUID keycloakId = keycloakAdminClient.createUser(
                dto.getEmail(), dto.getPassword(), dto.getName(), dto.getSurname());
        try {
            User saved = transactionTemplate.execute(status -> {
                User user = userMapper.toEntity(dto);
                user.setId(keycloakId);
                return userRepository.saveAndFlush(user);
            });
            return userMapper.toResponse(saved);
        } catch (RuntimeException ex) {
            compensateDeletedKeycloakUser(keycloakId);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#id")
    public UserResponseDto getById(UUID id) {
        return userMapper.toResponse(userRepository.findByIdWithCards(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND + id)));
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDto> getAll(String name, String surname, Boolean active, Pageable pageable) {
        Specification<User> spec = Specification.allOf();
        if (name != null && !name.isBlank()) {
            spec = spec.and(UserSpecifications.nameContains(name));
        }
        if (surname != null && !surname.isBlank()) {
            spec = spec.and(UserSpecifications.surnameContains(surname));
        }
        if (active != null) {
            spec = spec.and(UserSpecifications.isActive(active));
        }
        return userRepository.findAll(spec, pageable).map(userMapper::toResponse);
    }

    @Caching(evict = {
        @CacheEvict(value = "users", key = "#id"),
        @CacheEvict(value = "cards", key = "#id")
    })
    public UserResponseDto update(UUID id, UserUpdateDto dto) {
        UpdateResult result = transactionTemplate.execute(status -> updateLocally(id, dto));
        if (result.profileChanged()) {
            try {
                keycloakAdminClient.updateProfile(id, result.newEmail(), result.newName(), result.newSurname());
            } catch (RuntimeException ex) {
                revertProfile(id, result.oldEmail(), result.oldName(), result.oldSurname());
                throw ex;
            }
        }
        return result.response();
    }

    private UpdateResult updateLocally(UUID id, UserUpdateDto dto) {
        User user = findOrThrow(id);

        String newEmail = dto.getEmail() != null ? dto.getEmail() : user.getEmail();
        String newName = dto.getName() != null ? dto.getName() : user.getName();
        String newSurname = dto.getSurname() != null ? dto.getSurname() : user.getSurname();

        String oldEmail = user.getEmail();
        String oldName = user.getName();
        String oldSurname = user.getSurname();

        boolean emailChanged = !newEmail.equals(oldEmail);
        if (emailChanged && userRepository.existsByEmail(newEmail)) {
            throw new EmailAlreadyExistsException("Email already in use: " + newEmail);
        }
        boolean profileChanged = emailChanged
                || !Objects.equals(newName, oldName)
                || !Objects.equals(newSurname, oldSurname);

        userMapper.updateEntity(dto, user);
        UserResponseDto response = userMapper.toResponse(userRepository.save(user));
        return new UpdateResult(response, profileChanged, oldEmail, oldName, oldSurname,
                newEmail, newName, newSurname);
    }

    private void revertProfile(UUID id, String email, String name, String surname) {
        transactionTemplate.execute(status -> {
            User user = findOrThrow(id);
            user.setEmail(email);
            user.setName(name);
            user.setSurname(surname);
            userRepository.save(user);
            return null;
        });
    }

    @Caching(evict = {
        @CacheEvict(value = "users", key = "#id"),
        @CacheEvict(value = "cards", key = "#id")
    })
    public void delete(UUID id) {
        // Keycloak-first on purpose: deleting locally first would leave a Keycloak account
        // with no local profile on Keycloak failure — the ghost record from the task.
        // A failure of the local delete afterwards is logged so the ghost is detectable.
        keycloakAdminClient.deleteUser(id);
        try {
            transactionTemplate.execute(status -> {
                userRepository.delete(findOrThrow(id));
                return null;
            });
        } catch (RuntimeException ex) {
            log.error("Ghost account: Keycloak user {} was deleted, but the local delete failed", id, ex);
            throw ex;
        }
    }

    @Caching(evict = {
        @CacheEvict(value = "users", key = "#id"),
        @CacheEvict(value = "cards", key = "#id")
    })
    public UserResponseDto activate(UUID id) {
        UserResponseDto response = transactionTemplate.execute(status -> {
            User user = findOrThrow(id);
            user.setActive(true);
            return userMapper.toResponse(userRepository.save(user));
        });
        try {
            keycloakAdminClient.setEnabled(id, true);
        } catch (RuntimeException ex) {
            revertActive(id, false);
            throw ex;
        }
        return response;
    }

    @Caching(evict = {
        @CacheEvict(value = "users", key = "#id"),
        @CacheEvict(value = "cards", key = "#id")
    })
    public UserResponseDto deactivate(UUID id) {
        UserResponseDto response = transactionTemplate.execute(status -> {
            User user = findOrThrow(id);
            user.setActive(false);
            paymentCardRepository.deactivateAllByUserId(id);
            return userMapper.toResponse(userRepository.save(user));
        });
        try {
            keycloakAdminClient.setEnabled(id, false);
            keycloakAdminClient.logoutUser(id);
        } catch (RuntimeException ex) {
            transactionTemplate.execute(status -> {
                User user = findOrThrow(id);
                user.setActive(true);
                userRepository.save(user);
                paymentCardRepository.reactivateAllByUserId(id);
                return null;
            });
            throw ex;
        }
        return response;
    }

    private void revertActive(UUID id, boolean active) {
        transactionTemplate.execute(status -> {
            User user = findOrThrow(id);
            user.setActive(active);
            userRepository.save(user);
            return null;
        });
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND + id));
    }

    private void compensateDeletedKeycloakUser(UUID keycloakId) {
        try {
            keycloakAdminClient.deleteUser(keycloakId);
        } catch (RuntimeException compensationEx) {
            log.error("Ghost account: Keycloak user {} remains without a local profile "
                    + "because the compensating delete failed", keycloakId, compensationEx);
        }
    }

    private record UpdateResult(
            UserResponseDto response,
            boolean profileChanged,
            String oldEmail,
            String oldName,
            String oldSurname,
            String newEmail,
            String newName,
            String newSurname) {
    }
}
