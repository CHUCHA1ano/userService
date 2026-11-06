package com.innowise.userservice.service;

import com.innowise.userservice.exception.CardLimitExceededException;
import com.innowise.userservice.exception.EmailAlreadyExistsException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.integration.keycloak.KeycloakAdminClient;
import com.innowise.userservice.model.dto.CardCreateDto;
import com.innowise.userservice.model.dto.CardResponseDto;
import com.innowise.userservice.model.dto.CardUpdateDto;
import com.innowise.userservice.model.dto.UserCreateDto;
import com.innowise.userservice.model.dto.UserResponseDto;
import com.innowise.userservice.model.dto.UserUpdateDto;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.repository.spec.UserSpecifications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @InjectMocks
    private com.innowise.userservice.service.UserService userService;

    private UUID userId;
    private User user;
    private UserCreateDto createDto;
    private UserUpdateDto updateDto;
    private UserResponseDto responseDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setName("John");
        user.setSurname("Doe");
        user.setEmail("john@example.com");
        user.setActive(true);
        user.setBirthDate(LocalDate.of(1990, 1, 1));

        createDto = UserCreateDto.builder()
                .name("John")
                .surname("Doe")
                .email("john@example.com")
                .password("password123")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        updateDto = UserUpdateDto.builder()
                .name("Jane")
                .build();

        responseDto = UserResponseDto.builder()
                .id(userId)
                .name("John")
                .surname("Doe")
                .email("john@example.com")
                .active(true)
                .build();
    }

    @Test
    void create_shouldReturnUserResponseDto_whenDtoIsValid() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(keycloakAdminClient.createUser("john@example.com", "password123", "John", "Doe")).thenReturn(userId);
        when(userMapper.toEntity(any(UserCreateDto.class))).thenReturn(user);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(any(User.class))).thenReturn(responseDto);

        UserResponseDto result = userService.create(createDto);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        verify(userRepository).existsByEmail("john@example.com");
        verify(keycloakAdminClient).createUser("john@example.com", "password123", "John", "Doe");
        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void create_shouldThrowEmailAlreadyExistsException_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.create(createDto));
    }

    @Test
    void getById_shouldReturnUserResponseDto_whenUserExists() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(any(User.class))).thenReturn(responseDto);

        UserResponseDto result = userService.getById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
    }

    @Test
    void getById_shouldThrowUserNotFoundException_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getById(userId));
    }

    @Test
    void update_shouldReturnUpdatedUserResponseDto_whenUserExists() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(any(User.class))).thenReturn(responseDto);

        UserResponseDto result = userService.update(userId, updateDto);

        assertNotNull(result);
        verify(userMapper).updateEntity(updateDto, user);
        verify(userRepository).save(user);
    }

    @Test
    void update_shouldThrowUserNotFoundException_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.update(userId, updateDto));
    }

    @Test
    void delete_shouldDeleteUser_whenUserExists() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).delete(any(User.class));

        userService.delete(userId);

        verify(userRepository).delete(user);
    }

    @Test
    void delete_shouldThrowUserNotFoundException_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.delete(userId));
    }

    @Test
    void activate_shouldReturnActiveUserResponseDto_whenUserExists() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(any(User.class))).thenReturn(responseDto);

        UserResponseDto result = userService.activate(userId);

        assertNotNull(result);
        assertTrue(result.getActive());
    }

    @Test
    void activate_shouldThrowUserNotFoundException_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.activate(userId));
    }

    @Test
    void deactivate_shouldReturnInactiveUserResponseDto_whenUserExists() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setActive(false);
            return saved;
        });
        when(userMapper.toResponse(any(User.class))).thenAnswer(invocation -> {
            User entity = invocation.getArgument(0);
            return UserResponseDto.builder()
                    .id(entity.getId())
                    .name(entity.getName())
                    .surname(entity.getSurname())
                    .email(entity.getEmail())
                    .active(entity.getActive())
                    .birthDate(entity.getBirthDate())
                    .build();
        });
        when(paymentCardRepository.deactivateAllByUserId(any(UUID.class))).thenReturn(0);

        UserResponseDto result = userService.deactivate(userId);

        assertNotNull(result);
        assertFalse(result.getActive());
        verify(paymentCardRepository).deactivateAllByUserId(userId);
    }

    @Test
    void deactivate_shouldThrowUserNotFoundException_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.deactivate(userId));
    }

    @Test
    void getAll_shouldReturnPageOfUsers_whenFiltersProvided() {
        Page<User> userPage = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(userPage);
        when(userMapper.toResponse(any(User.class))).thenReturn(responseDto);

        Page<UserResponseDto> result = userService.getAll("John", "Doe", true, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }
}
