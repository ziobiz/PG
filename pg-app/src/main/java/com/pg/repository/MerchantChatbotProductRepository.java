package com.pg.repository;

import com.pg.entity.MerchantChatbotProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MerchantChatbotProductRepository extends JpaRepository<MerchantChatbotProduct, Long> {

    List<MerchantChatbotProduct> findByOrgUnitIdOrderBySortOrderAscIdAsc(Long orgUnitId);

    List<MerchantChatbotProduct> findByOrgUnitIdAndUseYnAndHqCatalogBlockYnOrderBySortOrderAscIdAsc(
            Long orgUnitId, String useYn, String hqCatalogBlockYn);

    List<MerchantChatbotProduct> findByOrgUnitIdInOrderByOrgUnitIdAscSortOrderAscIdAsc(Collection<Long> orgUnitIds);

    long countByOrgUnitId(Long orgUnitId);

    long countByOrgUnitIdAndUseYn(Long orgUnitId, String useYn);

    boolean existsByOrgUnitIdAndProductCode(Long orgUnitId, String productCode);
}
