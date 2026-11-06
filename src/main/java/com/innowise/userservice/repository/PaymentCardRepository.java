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

    @Modifying
    @Query(value = "update payment_cards set active = false where user_id = :userId",
            nativeQuery = true)
    int deactivateAllByUserId(UUID userId);
}
