package com.pg.repository;

import com.pg.entity.UserViewSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserViewSettingRepository extends JpaRepository<UserViewSetting, Long> {
    Optional<UserViewSetting> findByUsernameAndPageUrl(String username, String pageUrl);
}
