package innowise.spring.userService.model.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CardUpdateDto {
    @Pattern(regexp = "\\d{13,19}", message = "Card number must contain 13-19 digits")
    private String number;

    private String holder;

    @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}", message = "Expiration date must be in MM/yy format")
    private String expirationDate;
}
