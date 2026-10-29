package innowise.spring.userService.security;

import innowise.spring.userService.model.repository.PaymentCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * SpEL helper used by {@code @PreAuthorize} to check that a user accesses only their own resources.
 * Admins bypass these checks in the expressions themselves.
 */
@Component("access")
@RequiredArgsConstructor
public class ResourceAccessEvaluator {

    private final PaymentCardRepository paymentCardRepository;

    public boolean isSelf(UUID userId, Authentication authentication) {
        return subject(authentication)
                .map(sub -> sub.equals(userId.toString()))
                .orElse(false);
    }

    public boolean isCardOwner(UUID cardId, Authentication authentication) {
        Optional<String> subject = subject(authentication);
        if (subject.isEmpty()) {
            return false;
        }
        return paymentCardRepository.findById(cardId)
                .map(card -> card.getUser() != null
                        && subject.get().equals(card.getUser().getId().toString()))
                .orElse(false);
    }

    private Optional<String> subject(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return Optional.ofNullable(jwtAuthentication.getToken().getSubject());
        }
        return Optional.empty();
    }
}
