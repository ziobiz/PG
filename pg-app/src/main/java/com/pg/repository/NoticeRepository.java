package com.pg.repository;

import com.pg.entity.Notice;
import com.pg.entity.OrgLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /**
     * PostgreSQL: {@code (:x IS NULL OR col >= :x)} 는 null 바인딩 시 파라미터 타입을 추론하지 못함.
     * {@code COALESCE(:fromDt, n.regDt)} 로 미입력 시 조건을 항등으로 처리.
     */
    @Query("SELECT n FROM Notice n WHERE (:title IS NULL OR :title = '' OR LOWER(n.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "AND n.regDt >= COALESCE(:fromDt, n.regDt) AND n.regDt <= COALESCE(:toDt, n.regDt)")
    Page<Notice> search(@Param("title") String title, @Param("fromDt") java.time.LocalDateTime fromDt, @Param("toDt") java.time.LocalDateTime toDt, Pageable pageable);

    @Query("SELECT n FROM Notice n WHERE (:title IS NULL OR :title = '' OR LOWER(n.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "AND n.regDt >= COALESCE(:fromDt, n.regDt) AND n.regDt <= COALESCE(:toDt, n.regDt) " +
           "ORDER BY n.regDt DESC, n.id DESC")
    List<Notice> searchList(@Param("title") String title, @Param("fromDt") java.time.LocalDateTime fromDt, @Param("toDt") java.time.LocalDateTime toDt);

    Optional<Notice> findFirstByShowOnLoginOrderByRegDtDescIdDesc(String showOnLogin);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notice n SET n.showOnLogin = 'N' WHERE n.showOnLogin = 'Y'")
    int clearAllShowOnLogin();

    Optional<Notice> findFirstByShowAsPopupOrderByRegDtDescIdDesc(String showAsPopup);

    /** 로그인 페이지 접속팝업·첫화면 — 총본사 작성 공지만 */
    @Query("SELECT n FROM Notice n JOIN OrgUnit o ON n.orgUnitId = o.id "
            + "WHERE n.showAsPopup = :flag AND o.orgLevel = :writerLevel "
            + "ORDER BY n.regDt DESC, n.id DESC")
    List<Notice> findLoginSitePopupByWriterLevelOrderByRegDtDescIdDesc(
            @Param("flag") String flag, @Param("writerLevel") OrgLevel writerLevel, Pageable pageable);

    @Query("SELECT n FROM Notice n JOIN OrgUnit o ON n.orgUnitId = o.id "
            + "WHERE n.showOnLogin = :flag AND o.orgLevel = :writerLevel "
            + "ORDER BY n.regDt DESC, n.id DESC")
    List<Notice> findLoginSiteHomeByWriterLevelOrderByRegDtDescIdDesc(
            @Param("flag") String flag, @Param("writerLevel") OrgLevel writerLevel, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notice n SET n.showAsPopup = 'N' WHERE n.showAsPopup = 'Y'")
    int clearAllShowAsPopup();

    @Query("SELECT n FROM Notice n WHERE n.showPostLoginPopup = :flag ORDER BY n.regDt DESC, n.id DESC")
    java.util.List<Notice> findByShowPostLoginPopupOrderByRegDtDescIdDesc(@Param("flag") String flag);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notice n SET n.showPostLoginPopup = 'N' WHERE n.showPostLoginPopup = 'Y'")
    int clearAllShowPostLoginPopup();

    @Query("SELECT n FROM Notice n WHERE n.showOnMain = :flag ORDER BY n.regDt DESC, n.id DESC")
    java.util.List<Notice> findByShowOnMainOrderByRegDtDescIdDesc(@Param("flag") String flag);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notice n SET n.showOnMain = 'N' WHERE n.showOnMain = 'Y'")
    int clearAllShowOnMain();
}
