package innowise.spring.userService.service;

import innowise.spring.userService.exception.CardLimitExceededException;
import innowise.spring.userService.exception.EntityNotFoundException;
import innowise.spring.userService.model.dto.CardCreateDto;
import innowise.spring.userService.model.dto.CardResponseDto;
import innowise.spring.userService.model.dto.CardUpdateDto;
import innowise.spring.userService.model.dto.UserCreateDto;
import innowise.spring.userService.model.dto.UserResponseDto;
import innowise.spring.userService.model.dto.UserUpdateDto;
import innowise.spring.userService.model.entity.User;
import innowise.spring.userService.model.mapper.UserMapper;
import innowise.spring.userService.model.repository.PaymentCardRepository;
import innowise.spring.userService.model.repository.UserRepository;
import innowise.spring.userService.model.repository.spec.UserSpecifications;
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

    @InjectMocks
    private innowise.spring.userService.model.service.UserService userService;

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
        when(userMapper.toEntity(any(UserCreateDto.class))).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(any(User.class))).thenReturn(responseDto);

        UserResponseDto result = userService.create(createDto);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        verify(userRepository).existsByEmail("john@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_shouldThrowIllegalArgumentException_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.create(createDto));
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
    void getById_shouldThrowEntityNotFoundException_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.getById(userId));
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
    void update_shouldThrowEntityNotFoundException_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.update(userId, updateDto));
    }

    @Test
    void delete_shouldDeleteUser_whenUserExists() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).delete(any(User.class));

        userService.delete(userId);

        verify(userRepository).delete(user);
    }

    @Test
    void delete_shouldThrowEntityNotFoundException_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.delete(userId));
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
    void activate_shouldThrowEntityNotFoundException_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.activate(userId));
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
    void deactivate_shouldThrowEntityNotFoundException_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.deactivate(userId));
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
