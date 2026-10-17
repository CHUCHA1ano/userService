package innowise.spring.userService.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_cards")
public class PaymentCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "Card number cannot be empty")
    @Pattern(regexp = "\\d{13,19}", message = "Card number must contain 13-19 digits")
    @Column(nullable = false, length = 19)
    private String number;

    @NotBlank(message = "Card holder cannot be empty")
    @Column(nullable = false)
    private String holder;

    @NotBlank(message = "Expiration date cannot be empty")
    @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}", message = "Expiration date must be in MM/yy format")
    @Column(name = "expiration_date", nullable = false, length = 7)
    private String expirationDate;

    @NotNull(message = "Active flag cannot be null")
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}
