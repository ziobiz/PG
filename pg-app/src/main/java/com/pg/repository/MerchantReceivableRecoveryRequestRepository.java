package com.pg.repository;

import com.pg.entity.MerchantReceivableRecoveryRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MerchantReceivableRecoveryRequestRepository extends JpaRepository<MerchantReceivableRecoveryRequest, Long> {

    List<MerchantReceivableRecoveryRequest> findByMerchantIdAndStatusOrderByIdAsc(String merchantId, String status);

    List<MerchantReceivableRecoveryRequest> findByMerchantReceivableIdInAndStatus(Collection<Long> merchantReceivableIds, String status);

    boolean existsByMerchantReceivableIdAndStatus(Long merchantReceivableId, String status);
}
