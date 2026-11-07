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
import com.innowise.userservice.repository.spec.PaymentCardSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentCardService {

    private static final int MAX_CARDS_PER_USER = 5;
    private static final String CARDS_CACHE = "cards";
    private static final String USERS_CACHE = "users";

    private final PaymentCardRepository cardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper cardMapper;
    private final CacheManager cacheManager;

    @Transactional
    public CardResponseDto create(UUID userId, CardCreateDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new UserInactiveException("Cannot add a card to an inactive user: " + userId);
        }
        if (cardRepository.countByUserIdAndActiveTrue(userId) >= MAX_CARDS_PER_USER) {
            throw new CardLimitExceededException("User already has " + MAX_CARDS_PER_USER + " active cards");
        }
        PaymentCard card = cardMapper.toEntity(dto);
        card.setUser(user);
        CardResponseDto response = cardMapper.toResponse(cardRepository.save(card));
        evictUserCaches(userId);
        return response;
    }

    @Transactional(readOnly = true)
    public CardResponseDto getById(UUID id) {
        return cardMapper.toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "cards", key = "#userId")
    public List<CardResponseDto> getAllByUserId(UUID userId) {
        return cardRepository.findAllByUserId(userId).stream().map(cardMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<CardResponseDto> getAll(UUID userId, Boolean active, Pageable pageable) {
        Specification<PaymentCard> spec = Specification.allOf();
        if (userId != null) {
            spec = spec.and(PaymentCardSpecifications.belongsToUser(userId));
        }
        if (active != null) {
            spec = spec.and(PaymentCardSpecifications.isActive(active));
        }
        return cardRepository.findAll(spec, pageable).map(cardMapper::toResponse);
    }

    @Transactional
    public CardResponseDto update(UUID id, CardUpdateDto dto) {
        PaymentCard card = findOrThrow(id);
        cardMapper.updateEntity(dto, card);
        CardResponseDto response = cardMapper.toResponse(cardRepository.save(card));
        evictUserCaches(card.getUser().getId());
        return response;
    }

    @Transactional
    public void delete(UUID id) {
        PaymentCard card = findOrThrow(id);
        cardRepository.delete(card);
        evictUserCaches(card.getUser().getId());
    }

    @Transactional
    public CardResponseDto activate(UUID id) {
        PaymentCard card = findOrThrow(id);
        if (Boolean.TRUE.equals(card.getActive())) {
            CardResponseDto response = cardMapper.toResponse(card);
            evictUserCaches(card.getUser().getId());
            return response;
        }
        if (cardRepository.countByUserIdAndActiveTrue(card.getUser().getId()) >= MAX_CARDS_PER_USER) {
            throw new CardLimitExceededException("User already has " + MAX_CARDS_PER_USER + " active cards");
        }
        card.setActive(true);
        CardResponseDto response = cardMapper.toResponse(cardRepository.save(card));
        evictUserCaches(card.getUser().getId());
        return response;
    }

    @Transactional
    public CardResponseDto deactivate(UUID id) {
        PaymentCard card = findOrThrow(id);
        card.setActive(false);
        CardResponseDto response = cardMapper.toResponse(cardRepository.save(card));
        evictUserCaches(card.getUser().getId());
        return response;
    }

    private PaymentCard findOrThrow(UUID id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + id));
    }

    /**
     * A card mutation changes both the per-user card list and the user's aggregated
     * response (which includes its cards), so both cache entries are evicted.
     */
    private void evictUserCaches(UUID userId) {
        requireCache(CARDS_CACHE).evict(userId);
        requireCache(USERS_CACHE).evict(userId);
    }

    private Cache requireCache(String name) {
        Cache cache = cacheManager.getCache(name);
        if (cache == null) {
            throw new IllegalStateException("Cache is not configured: " + name);
        }
        return cache;
    }
}
