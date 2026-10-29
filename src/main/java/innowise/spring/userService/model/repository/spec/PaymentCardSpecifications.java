package innowise.spring.userService.model.repository.spec;

import innowise.spring.userService.model.entity.PaymentCard;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class PaymentCardSpecifications {

    private PaymentCardSpecifications() {
    }

    public static Specification<PaymentCard> belongsToUser(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<PaymentCard> isActive(Boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<PaymentCard> holderContains(String holder) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("holder")), "%" + holder.toLowerCase() + "%");
    }
}
