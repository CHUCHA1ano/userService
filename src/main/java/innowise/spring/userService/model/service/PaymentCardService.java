package innowise.spring.userService.model.service;

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
import innowise.spring.userService.model.repository.spec.PaymentCardSpecifications;
import lombok.RequiredArgsConstructor;
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
    public CardResponseDto create(CardCreateDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + dto.getUserId()));
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
        return cardMapper.toResponse(cardRepository.save(card));
    }

    @Transactional
    public void delete(UUID id) {
        cardRepository.delete(findOrThrow(id));
    }

    @Transactional
    public CardResponseDto activate(UUID id) {
        PaymentCard card = findOrThrow(id);
        if (cardRepository.countByUserIdAndActiveTrue(card.getUser().getId()) >= MAX_CARDS_PER_USER) {
            throw new CardLimitExceededException("User already has " + MAX_CARDS_PER_USER + " active cards");
        }
        card.setActive(true);
        return cardMapper.toResponse(cardRepository.save(card));
    }

    @Transactional
    public CardResponseDto deactivate(UUID id) {
        PaymentCard card = findOrThrow(id);
        card.setActive(false);
        return cardMapper.toResponse(cardRepository.save(card));
    }

    private PaymentCard findOrThrow(UUID id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Card not found: " + id));
    }
}
