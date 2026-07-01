package com.pg.repository;

import com.pg.entity.HqNotiWebhookPartner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HqNotiWebhookPartnerRepository extends JpaRepository<HqNotiWebhookPartner, Long> {

    List<HqNotiWebhookPartner> findByUseYnOrderBySortOrderAscIdAsc(String useYn);

    List<HqNotiWebhookPartner> findAllByOrderBySortOrderAscIdAsc();

    Optional<HqNotiWebhookPartner> findByPartnerCode(String partnerCode);
}
