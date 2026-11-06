package com.innowise.userservice.service;

import com.innowise.userservice.exception.CardLimitExceededException;
import com.innowise.userservice.exception.CardNotFoundException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.model.dto.CardCreateDto;
import com.innowise.userservice.model.dto.CardResponseDto;
import com.innowise.userservice.model.dto.CardUpdateDto;
import com.innowise.userservice.model.entity.PaymentCard;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
    private com.innowise.userservice.service.PaymentCardService cardService;

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
        card.setExpirationDate(LocalDate.of(2030, 12, 1));
        card.setActive(true);

        createDto = CardCreateDto.builder()
                .userId(userId)
                .number("4111111111111111")
                .holder("John Doe")
                .expirationDate(LocalDate.of(2030, 12, 1))
                .build();

        updateDto = CardUpdateDto.builder()
                .holder("Jane Doe")
                .build();

        responseDto = CardResponseDto.builder()
                .id(cardId)
                .userId(userId)
                .number("4111111111111111")
                .holder("John Doe")
                .expirationDate(LocalDate.of(2030, 12, 1))
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
    void create_shouldThrowUserNotFoundException_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> cardService.create(createDto));
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
    void getById_shouldThrowCardNotFoundException_whenCardNotFound() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () -> cardService.getById(cardId));
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
    void update_shouldThrowCardNotFoundException_whenCardNotFound() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () -> cardService.update(cardId, updateDto));
    }

    @Test
    void delete_shouldDeleteCard_whenCardExists() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        doNothing().when(cardRepository).delete(any(PaymentCard.class));

        cardService.delete(cardId);

        verify(cardRepository).delete(card);
    }

    @Test
    void delete_shouldThrowCardNotFoundException_whenCardNotFound() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () -> cardService.delete(cardId));
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
    void deactivate_shouldThrowCardNotFoundException_whenCardNotFound() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () -> cardService.deactivate(cardId));
    }
}
