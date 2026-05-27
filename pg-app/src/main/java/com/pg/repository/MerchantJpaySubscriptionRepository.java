package com.pg.repository;

import com.pg.entity.MerchantJpaySubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MerchantJpaySubscriptionRepository extends JpaRepository<MerchantJpaySubscription, Long> {

    Optional<MerchantJpaySubscription> findByCompCodeAndCheckoutOrderNo(String compCode, String checkoutOrderNo);

    List<MerchantJpaySubscription> findTop200ByOrgUnitIdOrderByCreatedAtDesc(Long orgUnitId);

    List<MerchantJpaySubscription> findTop500ByOrderByCreatedAtDesc();
}
