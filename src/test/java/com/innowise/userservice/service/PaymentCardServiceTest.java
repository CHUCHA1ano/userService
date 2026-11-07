package com.innowise.userservice.service;

import com.innowise.userservice.exception.CardLimitExceededException;
import com.innowise.userservice.exception.CardNotFoundException;
import com.innowise.userservice.exception.UserInactiveException;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

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

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cardsCache;

    @Mock
    private Cache usersCache;

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

        lenient().when(cacheManager.getCache("cards")).thenReturn(cardsCache);
        lenient().when(cacheManager.getCache("users")).thenReturn(usersCache);
    }

    @Test
    void create_shouldReturnCardResponseDto_whenDtoIsValid() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardRepository.countByUserIdAndActiveTrue(userId)).thenReturn(0L);
        when(cardMapper.toEntity(any(CardCreateDto.class))).thenReturn(card);
        when(cardRepository.save(any(PaymentCard.class))).thenReturn(card);
        when(cardMapper.toResponse(any(PaymentCard.class))).thenReturn(responseDto);

        CardResponseDto result = cardService.create(userId, createDto);

        assertNotNull(result);
        assertEquals(cardId, result.getId());
        assertEquals(userId, result.getUserId());
        verify(cardRepository).countByUserIdAndActiveTrue(userId);
        verify(cardRepository).save(any(PaymentCard.class));
        verify(cardsCache).evict(userId);
        verify(usersCache).evict(userId);
    }

    @Test
    void create_shouldThrowCardLimitExceededException_whenLimitReached() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardRepository.countByUserIdAndActiveTrue(userId)).thenReturn(5L);

        assertThrows(CardLimitExceededException.class, () -> cardService.create(userId, createDto));
    }

    @Test
    void create_shouldThrowUserNotFoundException_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> cardService.create(userId, createDto));
    }

    @Test
    void create_shouldThrowUserInactiveException_whenUserIsNotActive() {
        user.setActive(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(UserInactiveException.class, () -> cardService.create(userId, createDto));
        verifyNoInteractions(cardRepository, cardMapper);
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
        verify(cardsCache).evict(userId);
        verify(usersCache).evict(userId);
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
        card.setActive(false);
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
        card.setActive(false);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.countByUserIdAndActiveTrue(userId)).thenReturn(5L);

        assertThrows(CardLimitExceededException.class, () -> cardService.activate(cardId));
    }

    @Test
    void activate_shouldReturnCardImmediately_whenAlreadyActive() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardMapper.toResponse(card)).thenReturn(responseDto);

        CardResponseDto result = cardService.activate(cardId);

        assertNotNull(result);
        assertEquals(cardId, result.getId());
        verify(cardRepository, never()).countByUserIdAndActiveTrue(any());
        verify(cardRepository, never()).save(any());
        verify(cardsCache).evict(userId);
        verify(usersCache).evict(userId);
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
