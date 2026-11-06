package com.innowise.userservice.repository;

import com.innowise.userservice.model.entity.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCard, UUID>, JpaSpecificationExecutor<PaymentCard> {

    List<PaymentCard> findAllByUserId(UUID userId);

    long countByUserIdAndActiveTrue(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update payment_cards set active = false, updated_at = now() where user_id = :userId",
            nativeQuery = true)
    int deactivateAllByUserId(UUID userId);

    /**
     * Reverts {@link #deactivateAllByUserId} when the Keycloak side of the
     * user-deactivation flow fails. Note: this also re-enables cards that were
     * deactivated individually before, local consistency wins for the revert.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update payment_cards set active = true, updated_at = now() where user_id = :userId",
            nativeQuery = true)
    int reactivateAllByUserId(UUID userId);
}
