package innowise.spring.userService.model.mapper;

import innowise.spring.userService.model.dto.CardCreateDto;
import innowise.spring.userService.model.dto.CardResponseDto;
import innowise.spring.userService.model.dto.CardUpdateDto;
import innowise.spring.userService.model.entity.PaymentCard;
import innowise.spring.userService.model.entity.User;
import org.springframework.stereotype.Component;

@Component
public class PaymentCardMapper {

    public PaymentCard toEntity(CardCreateDto dto) {
        if (dto == null) {
            return null;
        }
        return PaymentCard.builder()
                .user(dto.getUserId() != null ? User.builder().id(dto.getUserId()).build() : null)
                .number(dto.getNumber())
                .holder(dto.getHolder())
                .expirationDate(dto.getExpirationDate())
                .build();
    }

    public CardResponseDto toResponse(PaymentCard entity) {
        if (entity == null) {
            return null;
        }
        return CardResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .number(entity.getNumber())
                .holder(entity.getHolder())
                .expirationDate(entity.getExpirationDate())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntity(CardUpdateDto dto, PaymentCard entity) {
        if (dto == null || entity == null) {
            return;
        }
        if (dto.getNumber() != null) {
            entity.setNumber(dto.getNumber());
        }
        if (dto.getHolder() != null) {
            entity.setHolder(dto.getHolder());
        }
        if (dto.getExpirationDate() != null) {
            entity.setExpirationDate(dto.getExpirationDate());
        }
    }
}
