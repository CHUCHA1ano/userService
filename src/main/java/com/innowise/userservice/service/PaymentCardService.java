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
import com.innowise.userservice.repository.spec.PaymentCardSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
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

    private final PaymentCardRepository cardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper cardMapper;

    @Transactional
    @CacheEvict(value = "cards", key = "#dto.userId")
    public CardResponseDto create(CardCreateDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + dto.getUserId()));
        if (cardRepository.countByUserIdAndActiveTrue(dto.getUserId()) >= MAX_CARDS_PER_USER) {
            throw new CardLimitExceededException("User already has " + MAX_CARDS_PER_USER + " active cards");
        }
        PaymentCard card = cardMapper.toEntity(dto);
        card.setUser(user);
        return cardMapper.toResponse(cardRepository.save(card));
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
    @CacheEvict(value = "cards", allEntries = true)
    public CardResponseDto update(UUID id, CardUpdateDto dto) {
        PaymentCard card = findOrThrow(id);
        cardMapper.updateEntity(dto, card);
        return cardMapper.toResponse(cardRepository.save(card));
    }

    @Transactional
    @CacheEvict(value = "cards", allEntries = true)
    public void delete(UUID id) {
        cardRepository.delete(findOrThrow(id));
    }

    @Transactional
    @CacheEvict(value = "cards", allEntries = true)
    public CardResponseDto activate(UUID id) {
        PaymentCard card = findOrThrow(id);
        if (cardRepository.countByUserIdAndActiveTrue(card.getUser().getId()) >= MAX_CARDS_PER_USER) {
            throw new CardLimitExceededException("User already has " + MAX_CARDS_PER_USER + " active cards");
        }
        card.setActive(true);
        return cardMapper.toResponse(cardRepository.save(card));
    }

    @Transactional
    @CacheEvict(value = "cards", allEntries = true)
    public CardResponseDto deactivate(UUID id) {
        PaymentCard card = findOrThrow(id);
        card.setActive(false);
        return cardMapper.toResponse(cardRepository.save(card));
    }

    private PaymentCard findOrThrow(UUID id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + id));
    }
}
