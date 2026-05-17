package com.pg.repository;

import com.pg.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /**
     * PostgreSQL: {@code (:x IS NULL OR col >= :x)} 는 null 바인딩 시 파라미터 타입을 추론하지 못함.
     * {@code COALESCE(:fromDt, n.regDt)} 로 미입력 시 조건을 항등으로 처리.
     */
    @Query("SELECT n FROM Notice n WHERE (:title IS NULL OR :title = '' OR LOWER(n.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "AND n.regDt >= COALESCE(:fromDt, n.regDt) AND n.regDt <= COALESCE(:toDt, n.regDt)")
    Page<Notice> search(@Param("title") String title, @Param("fromDt") java.time.LocalDateTime fromDt, @Param("toDt") java.time.LocalDateTime toDt, Pageable pageable);

    Optional<Notice> findFirstByShowOnLoginOrderByRegDtDescIdDesc(String showOnLogin);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notice n SET n.showOnLogin = 'N' WHERE n.showOnLogin = 'Y'")
    int clearAllShowOnLogin();

    Optional<Notice> findFirstByShowAsPopupOrderByRegDtDescIdDesc(String showAsPopup);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notice n SET n.showAsPopup = 'N' WHERE n.showAsPopup = 'Y'")
    int clearAllShowAsPopup();
}
