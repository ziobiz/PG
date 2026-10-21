package com.pg.repository;

import com.pg.entity.HqRemitAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HqRemitAccountRepository extends JpaRepository<HqRemitAccount, Long> {

    Optional<HqRemitAccount> findByRecipientId(String recipientId);

    List<HqRemitAccount> findByActiveYnOrderByDefaultYnDescIdAsc(String activeYn);

    long countByDefaultYnAndActiveYn(String defaultYn, String activeYn);
}
