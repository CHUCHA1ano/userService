package innowise.spring.userService.model.service;

import innowise.spring.userService.exception.EntityNotFoundException;
import innowise.spring.userService.model.dto.UserCreateDto;
import innowise.spring.userService.model.dto.UserResponseDto;
import innowise.spring.userService.model.dto.UserUpdateDto;
import innowise.spring.userService.model.entity.User;
import innowise.spring.userService.model.mapper.UserMapper;
import innowise.spring.userService.model.repository.PaymentCardRepository;
import innowise.spring.userService.model.repository.UserRepository;
import innowise.spring.userService.model.repository.spec.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String USER_NOT_FOUND = "User not found: ";

    private final UserRepository userRepository;
    private final PaymentCardRepository paymentCardRepository;
    private final UserMapper userMapper;

    public UserResponseDto create(UserCreateDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already in use: " + dto.getEmail());
        }
        User saved = userRepository.save(userMapper.toEntity(dto));
        return userMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
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
    public UserResponseDto update(UUID id, UserUpdateDto dto) {
        User user = findOrThrow(id);
        userMapper.updateEntity(dto, user);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(UUID id) {
        User user = findOrThrow(id);
        userRepository.delete(user);
    }

    @Transactional
    public UserResponseDto activate(UUID id) {
        User user = findOrThrow(id);
        user.setActive(true);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponseDto deactivate(UUID id) {
        User user = findOrThrow(id);
        user.setActive(false);
        paymentCardRepository.deactivateAllByUserId(id);
        return userMapper.toResponse(userRepository.save(user));
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND + id));
    }
}
