package innowise.spring.userService.service;

import innowise.spring.userService.exception.CardLimitExceededException;
import innowise.spring.userService.exception.EntityNotFoundException;
import innowise.spring.userService.model.dto.CardCreateDto;
import innowise.spring.userService.model.dto.CardResponseDto;
import innowise.spring.userService.model.dto.CardUpdateDto;
import innowise.spring.userService.model.entity.PaymentCard;
import innowise.spring.userService.model.entity.User;
import innowise.spring.userService.model.mapper.PaymentCardMapper;
import innowise.spring.userService.model.repository.PaymentCardRepository;
import innowise.spring.userService.model.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceTest {

    @Mock
    private PaymentCardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardMapper cardMapper;

    @InjectMocks
    private innowise.spring.userService.model.service.PaymentCardService cardService;

    private UUID userId;
    private UUID cardId;
    private User user;
    private PaymentCard card;
    private CardCreateDto createDto;
    private CardUpdateDto updateDto;
    private CardResponseDto responseDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        cardId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setName("John");
        user.setSurname("Doe");

        card = new PaymentCard();
        card.setId(cardId);
        card.setUser(user);
        card.setNumber("4111111111111111");
        card.setHolder("John Doe");
        card.setExpirationDate("12/30");
        card.setActive(true);

        createDto = CardCreateDto.builder()
                .userId(userId)
                .number("4111111111111111")
                .holder("John Doe")
                .expirationDate("12/30")
                .build();

        updateDto = CardUpdateDto.builder()
                .holder("Jane Doe")
                .build();

        responseDto = CardResponseDto.builder()
                .id(cardId)
                .userId(userId)
                .number("4111111111111111")
                .holder("John Doe")
                .expirationDate("12/30")
                .active(true)
                .build();
    }

    @Test
    void create_shouldReturnCardResponseDto_whenDtoIsValid() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardRepository.countByUserIdAndActiveTrue(userId)).thenReturn(0L);
        when(cardMapper.toEntity(any(CardCreateDto.class))).thenReturn(card);
        when(cardRepository.save(any(PaymentCard.class))).thenReturn(card);
        when(cardMapper.toResponse(any(PaymentCard.class))).thenReturn(responseDto);

        CardResponseDto result = cardService.create(createDto);

        assertNotNull(result);
        assertEquals(cardId, result.getId());
        assertEquals(userId, result.getUserId());
        verify(cardRepository).countByUserIdAndActiveTrue(userId);
        verify(cardRepository).save(any(PaymentCard.class));
    }

    @Test
    void create_shouldThrowCardLimitExceededException_whenLimitReached() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardRepository.countByUserIdAndActiveTrue(userId)).thenReturn(5L);

        assertThrows(CardLimitExceededException.class, () -> cardService.create(createDto));
    }

    @Test
    void create_shouldThrowEntityNotFoundException_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> cardService.create(createDto));
    }

    @Test
    void getById_shouldReturnCardResponseDto_whenCardExists() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardMapper.toResponse(any(PaymentCard.class))).thenReturn(responseDto);

        CardResponseDto result = cardService.getById(cardId);

        assertNotNull(result);
        assertEquals(cardId, result.getId());
    }

    @Test
    void getById_shouldThrowEntityNotFoundException_whenCardNotFound() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> cardService.getById(cardId));
    }

    @Test
    void getAllByUserId_shouldReturnListOfCards_whenUserHasCards() {
        when(cardRepository.findAllByUserId(userId)).thenReturn(List.of(card));
        when(cardMapper.toResponse(any(PaymentCard.class))).thenReturn(responseDto);

        List<CardResponseDto> result = cardService.getAllByUserId(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(cardId, result.get(0).getId());
    }

    @Test
    void update_shouldReturnUpdatedCardResponseDto_whenCardExists() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(PaymentCard.class))).thenReturn(card);
        when(cardMapper.toResponse(any(PaymentCard.class))).thenReturn(responseDto);

        CardResponseDto result = cardService.update(cardId, updateDto);

        assertNotNull(result);
        verify(cardMapper).updateEntity(updateDto, card);
        verify(cardRepository).save(card);
    }

    @Test
    void update_shouldThrowEntityNotFoundException_whenCardNotFound() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> cardService.update(cardId, updateDto));
    }

    @Test
    void delete_shouldDeleteCard_whenCardExists() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        doNothing().when(cardRepository).delete(any(PaymentCard.class));

        cardService.delete(cardId);

        verify(cardRepository).delete(card);
    }

    @Test
    void delete_shouldThrowEntityNotFoundException_whenCardNotFound() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> cardService.delete(cardId));
    }

    @Test
    void activate_shouldReturnActiveCardResponseDto_whenCardExists() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.countByUserIdAndActiveTrue(userId)).thenReturn(0L);
        when(cardRepository.save(any(PaymentCard.class))).thenReturn(card);
        when(cardMapper.toResponse(any(PaymentCard.class))).thenReturn(responseDto);

        CardResponseDto result = cardService.activate(cardId);

        assertNotNull(result);
        assertTrue(result.getActive());
    }

    @Test
    void activate_shouldThrowCardLimitExceededException_whenLimitReached() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.countByUserIdAndActiveTrue(userId)).thenReturn(5L);

        assertThrows(CardLimitExceededException.class, () -> cardService.activate(cardId));
    }

    @Test
    void deactivate_shouldReturnInactiveCardResponseDto_whenCardExists() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(PaymentCard.class))).thenAnswer(invocation -> {
            PaymentCard saved = invocation.getArgument(0);
            saved.setActive(false);
            return saved;
        });
        when(cardMapper.toResponse(any(PaymentCard.class))).thenAnswer(invocation -> {
            PaymentCard entity = invocation.getArgument(0);
            return CardResponseDto.builder()
                    .id(entity.getId())
                    .userId(entity.getUser().getId())
                    .number(entity.getNumber())
                    .holder(entity.getHolder())
                    .expirationDate(entity.getExpirationDate())
                    .active(entity.getActive())
                    .build();
        });

        CardResponseDto result = cardService.deactivate(cardId);

        assertNotNull(result);
        assertFalse(result.getActive());
    }

    @Test
    void deactivate_shouldThrowEntityNotFoundException_whenCardNotFound() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> cardService.deactivate(cardId));
    }
}
