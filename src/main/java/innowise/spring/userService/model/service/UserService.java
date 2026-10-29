package innowise.spring.userService.model.service;

import innowise.spring.userService.exception.EntityNotFoundException;
import innowise.spring.userService.integration.keycloak.KeycloakAdminClient;
import innowise.spring.userService.model.dto.UserCreateDto;
import innowise.spring.userService.model.dto.UserResponseDto;
import innowise.spring.userService.model.dto.UserUpdateDto;
import innowise.spring.userService.model.entity.User;
import innowise.spring.userService.model.mapper.UserMapper;
import innowise.spring.userService.model.repository.PaymentCardRepository;
import innowise.spring.userService.model.repository.UserRepository;
import innowise.spring.userService.model.repository.spec.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String USER_NOT_FOUND = "User not found: ";

    private final UserRepository userRepository;
    private final PaymentCardRepository paymentCardRepository;
    private final UserMapper userMapper;
    private final KeycloakAdminClient keycloakAdminClient;

    @Transactional
    public UserResponseDto create(UserCreateDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already in use: " + dto.getEmail());
        }

        // Keycloak owns credentials and generates the user id. It is reused as the local id,
        // so the JWT subject matches the id used by user-scoped endpoints.
        UUID keycloakId = keycloakAdminClient.createUser(
                dto.getEmail(), dto.getPassword(), dto.getName(), dto.getSurname());
        try {
            User user = userMapper.toEntity(dto);
            user.setId(keycloakId);
            User saved = userRepository.saveAndFlush(user);
            return userMapper.toResponse(saved);
        } catch (RuntimeException ex) {
            try {
                keycloakAdminClient.deleteUser(keycloakId);
            } catch (RuntimeException ignored) {
                // best-effort compensation
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#id")
    public UserResponseDto getById(UUID id) {
        return userMapper.toResponse(findOrThrow(id));
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

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#id"),
        @CacheEvict(value = "cards", key = "#id")
    })
    public UserResponseDto update(UUID id, UserUpdateDto dto) {
        User user = findOrThrow(id);

        String newEmail = dto.getEmail() != null ? dto.getEmail() : user.getEmail();
        String newName = dto.getName() != null ? dto.getName() : user.getName();
        String newSurname = dto.getSurname() != null ? dto.getSurname() : user.getSurname();

        boolean emailChanged = !newEmail.equals(user.getEmail());
        if (emailChanged && userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Email already in use: " + newEmail);
        }
        boolean profileChanged = emailChanged
                || !Objects.equals(newName, user.getName())
                || !Objects.equals(newSurname, user.getSurname());
        if (profileChanged) {
            keycloakAdminClient.updateProfile(id, newEmail, newName, newSurname);
        }

        userMapper.updateEntity(dto, user);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#id"),
        @CacheEvict(value = "cards", key = "#id")
    })
    public void delete(UUID id) {
        User user = findOrThrow(id);
        keycloakAdminClient.deleteUser(id);
        userRepository.delete(user);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#id"),
        @CacheEvict(value = "cards", key = "#id")
    })
    public UserResponseDto activate(UUID id) {
        User user = findOrThrow(id);
        keycloakAdminClient.setEnabled(id, true);
        user.setActive(true);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#id"),
        @CacheEvict(value = "cards", key = "#id")
    })
    public UserResponseDto deactivate(UUID id) {
        User user = findOrThrow(id);
        keycloakAdminClient.setEnabled(id, false);
        // Access tokens live for 12 hours; revoke the sessions so the change takes effect now.
        keycloakAdminClient.logoutUser(id);
        user.setActive(false);
        paymentCardRepository.deactivateAllByUserId(id);
        return userMapper.toResponse(userRepository.save(user));
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND + id));
    }
}
