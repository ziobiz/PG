package com.pg.repository;

import com.pg.entity.OutcomeReasonTranslationCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OutcomeReasonTranslationCacheRepository extends JpaRepository<OutcomeReasonTranslationCache, Long> {

    List<OutcomeReasonTranslationCache> findByCacheKeyIn(Collection<String> cacheKeys);

    Optional<OutcomeReasonTranslationCache> findByCacheKey(String cacheKey);
}
