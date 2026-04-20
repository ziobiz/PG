package com.pg.service;

import com.pg.entity.AppUser;
import com.pg.entity.OrgUnitChangeLog;
import com.pg.repository.OrgUnitChangeLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 업체변경이력(tb_org_unit_change_log) 공통 기록·조회.
 * 화면별(업체정보·도메인·권한·정산·수수료 등) 저장 시 동일 그리드에 표시되도록 한다.
 */
@Service
public class OrgUnitChangeAuditService {

    private final OrgUnitChangeLogRepository orgUnitChangeLogRepository;

    public OrgUnitChangeAuditService(OrgUnitChangeLogRepository orgUnitChangeLogRepository) {
        this.orgUnitChangeLogRepository = orgUnitChangeLogRepository;
    }

    public String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser u) {
            return u.getUsername() != null ? u.getUsername() : "";
        }
        return auth != null && auth.getName() != null ? auth.getName() : "system";
    }

    public Page<OrgUnitChangeLog> findAll(Specification<OrgUnitChangeLog> spec, Pageable pageable) {
        return orgUnitChangeLogRepository.findAll(spec, pageable);
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * 별도 트랜잭션으로 기록 — 호출부 트랜잭션 롤백과 무관하게 이력 남김을 방지하려면 호출부에서만 사용하지 말고
     * 일반적으로는 호출 서비스의 트랜잭션에 참여한다.
     */
    @Transactional
    public void appendIfChanged(long orgUnitId, String compId, String compNm, String fieldLabel,
                                String before, String after) {
        if (Objects.equals(nz(before), nz(after))) {
            return;
        }
        OrgUnitChangeLog e = new OrgUnitChangeLog();
        e.setOrgUnitId(orgUnitId);
        e.setCompId(compId != null ? compId.trim() : "");
        e.setCompNm(compNm != null ? compNm.trim() : "");
        e.setFieldLabel(fieldLabel != null ? fieldLabel : "");
        e.setValueBefore(before != null ? before : "");
        e.setValueAfter(after != null ? after : "");
        e.setChangedBy(currentActor());
        orgUnitChangeLogRepository.save(e);
    }

    @Transactional
    public void appendAll(List<OrgUnitChangeLog> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        String actor = currentActor();
        for (OrgUnitChangeLog r : rows) {
            if (r.getChangedBy() == null || r.getChangedBy().isBlank()) {
                r.setChangedBy(actor);
            }
            if (r.getValueBefore() == null) {
                r.setValueBefore("");
            }
            if (r.getValueAfter() == null) {
                r.setValueAfter("");
            }
        }
        orgUnitChangeLogRepository.saveAll(rows);
    }
}
