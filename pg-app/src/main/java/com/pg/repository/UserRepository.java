package com.pg.repository;

import com.pg.entity.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByUsernameIgnoreCase(String username);
    List<AppUser> findByOrgUnitCode(String orgUnitCode);
    List<AppUser> findByOrgUnitCodeAndUsernameNot(String orgUnitCode, String username);

    List<AppUser> findByAssistantRoleTypeIgnoreCase(String assistantRoleType);

    Page<AppUser> findByUsernameContainingAndNameContaining(String username, String name, Pageable pageable);

    @Query("SELECT u FROM AppUser u WHERE " +
            "(:uid = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :uid, '%'))) AND " +
            "(:nm = '' OR LOWER(COALESCE(u.name, '')) LIKE LOWER(CONCAT('%', :nm, '%'))) AND " +
            "(:cc = '' OR LOWER(COALESCE(u.orgUnitCode, '')) LIKE LOWER(CONCAT('%', :cc, '%'))) AND " +
            "(:st = '' OR COALESCE(u.userStatus, 'ACTIVE') = :st)")
    Page<AppUser> searchForList(@Param("uid") String uid, @Param("nm") String nm, @Param("cc") String cc,
                               @Param("st") String st, Pageable pageable);
}
