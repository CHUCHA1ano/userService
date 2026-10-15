package innowise.spring.userService.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class CardResponseDto {
    private UUID id;
    private UUID userId;
    private String number;
    private String holder;
    private String expirationDate;
    private Boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
