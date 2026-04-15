package com.pg.repository;

import com.pg.entity.MerchantReceivable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MerchantReceivableRepository extends JpaRepository<MerchantReceivable, Long>, JpaSpecificationExecutor<MerchantReceivable> {

    List<MerchantReceivable> findByMerchantIdAndStatusInOrderByIdAsc(String merchantId, Collection<String> statuses);

    List<MerchantReceivable> findByMerchantIdOrderByIdDesc(String merchantId);

    boolean existsByMerchantIdAndReasonCodeAndMemo(String merchantId, String reasonCode, String memo);

    Optional<MerchantReceivable> findByMerchantIdAndReasonCodeAndMemo(String merchantId, String reasonCode, String memo);

    List<MerchantReceivable> findByReasonCodeAndMemoIn(String reasonCode, Collection<String> memos);
}
