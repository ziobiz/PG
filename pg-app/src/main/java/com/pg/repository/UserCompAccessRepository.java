package com.pg.repository;

import com.pg.entity.UserCompAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCompAccessRepository extends JpaRepository<UserCompAccess, Long> {

    List<UserCompAccess> findAllByOrderByUsernameAscCompCodeAsc();
}
