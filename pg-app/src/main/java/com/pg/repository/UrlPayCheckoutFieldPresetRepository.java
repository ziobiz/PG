package com.pg.repository;

import com.pg.entity.UrlPayCheckoutFieldPreset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UrlPayCheckoutFieldPresetRepository extends JpaRepository<UrlPayCheckoutFieldPreset, Long> {

    List<UrlPayCheckoutFieldPreset> findAllByOrderBySortNoAscIdAsc();

    Optional<UrlPayCheckoutFieldPreset> findFirstByIsDefaultYnIgnoreCase(String isDefaultYn);

    Optional<UrlPayCheckoutFieldPreset> findByPresetName(String presetName);

    boolean existsByPresetName(String presetName);
}
