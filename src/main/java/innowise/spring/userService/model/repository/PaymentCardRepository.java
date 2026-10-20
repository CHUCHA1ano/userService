package innowise.spring.userService.model.repository;

import innowise.spring.userService.model.entity.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCard, UUID>, JpaSpecificationExecutor<PaymentCard> {

    // named (derived) methods
    List<PaymentCard> findAllByUserId(UUID userId);

    List<PaymentCard> findAllByUserIdAndActiveTrue(UUID userId);

    long countByUserIdAndActiveTrue(UUID userId);

    //JPQL
    @Query("select c from PaymentCard c where c.user.id = :userId and c.active = true")
    List<PaymentCard> findActiveByUserId(UUID userId);

    //native SQL
    @Query(value = "select * from payment_cards where user_id = :userId order by created_at desc",
            nativeQuery = true)
    List<PaymentCard> findAllByUserIdOrderByCreatedDesc(UUID userId);

    @Modifying
    @Query(value = "update payment_cards set active = false where user_id = :userId",
            nativeQuery = true)
    int deactivateAllByUserId(UUID userId);
}
