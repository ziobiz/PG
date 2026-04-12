package com.pg.repository;

import com.pg.entity.MerchantReceivable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface MerchantReceivableRepository extends JpaRepository<MerchantReceivable, Long>, JpaSpecificationExecutor<MerchantReceivable> {

    List<MerchantReceivable> findByMerchantIdAndStatusInOrderByIdAsc(String merchantId, Collection<String> statuses);

    List<MerchantReceivable> findByMerchantIdOrderByIdDesc(String merchantId);
}
