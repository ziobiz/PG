package com.pg.repository;

import com.pg.entity.MerchantIlkSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MerchantIlkSubscriptionRepository extends JpaRepository<MerchantIlkSubscription, Long> {

    Optional<MerchantIlkSubscription> findByCompIdAndSubscriptionNo(String compId, String subscriptionNo);

    List<MerchantIlkSubscription> findTop50ByStatusAndNextChargeAtLessThanEqualOrderByNextChargeAtAsc(
            String status, LocalDateTime nextChargeAt);

    List<MerchantIlkSubscription> findTop200ByOrgUnitIdOrderByCreatedAtDesc(Long orgUnitId);

    Optional<MerchantIlkSubscription> findFirstBySubscriptionNoOrderByIdDesc(String subscriptionNo);

    Optional<MerchantIlkSubscription> findFirstByFirstOrderNoOrderByIdDesc(String firstOrderNo);
}
