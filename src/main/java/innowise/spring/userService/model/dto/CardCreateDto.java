package innowise.spring.userService.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class CardCreateDto {
    @NotNull(message = "User id cannot be null")
    private UUID userId;

    @NotBlank(message = "Card number cannot be empty")
    @Pattern(regexp = "\\d{13,19}", message = "Card number must contain 13-19 digits")
    private String number;

    @NotBlank(message = "Card holder cannot be empty")
    private String holder;

    @NotBlank(message = "Expiration date cannot be empty")
    @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}", message = "Expiration date must be in MM/yy format")
    private String expirationDate;
}
