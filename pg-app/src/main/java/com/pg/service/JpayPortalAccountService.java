package com.pg.service;

import com.pg.entity.JpayPortalAccount;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.JpayPortalAccountRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class JpayPortalAccountService {

    private final JpayPortalAccountRepository repository;
    private final OrgUnitRepository orgUnitRepository;
    private final PgAgencyRepository pgAgencyRepository;

    public JpayPortalAccountService(JpayPortalAccountRepository repository,
                                    OrgUnitRepository orgUnitRepository,
                                    PgAgencyRepository pgAgencyRepository) {
        this.repository = repository;
        this.orgUnitRepository = orgUnitRepository;
        this.pgAgencyRepository = pgAgencyRepository;
    }

    public Map<String, Object> listForAdmin() {
        Map<String, Object> out = new LinkedHashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (JpayPortalAccount a : repository.findAllByOrderBySortOrderAscIdAsc()) {
            rows.add(toAdminMap(a));
        }
        out.put("list", rows);
        out.put("masterDistOptions", listMasterDistOptions());
        out.put("jpayPgOptions", listJpayPgOptions());
        return out;
    }

    public List<JpayPortalAccount> listActiveForSync() {
        return repository.findByUseYnOrderBySortOrderAscIdAsc("Y");
    }

    @Transactional
    public Map<String, Object> save(Map<String, Object> body) {
        if (body == null) {
            throw new IllegalArgumentException("요청 본문이 비어 있습니다.");
        }
        Long id = parseLong(body.get("id"));
        Long masterOrgUnitId = parseLong(body.get("masterOrgUnitId"));
        if (masterOrgUnitId == null) {
            throw new IllegalArgumentException("총판(조직)을 선택하세요.");
        }
        OrgUnit master = orgUnitRepository.findById(masterOrgUnitId)
                .orElseThrow(() -> new IllegalArgumentException("총판 조직을 찾을 수 없습니다."));
        if (master.getOrgLevel() != OrgLevel.MASTER_DIST) {
            throw new IllegalArgumentException("총판(MASTER_DIST) 조직만 등록할 수 있습니다.");
        }
        String masterCode = master.getCode() != null ? master.getCode().trim() : "";
        if (masterCode.isEmpty()) {
            throw new IllegalArgumentException("총판 업체코드가 없습니다.");
        }

        JpayPortalAccount entity;
        if (id != null) {
            entity = repository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다."));
        } else {
            entity = new JpayPortalAccount();
        }

        String username = trim(body.get("portalUsername"));
        if (username.isEmpty()) {
            throw new IllegalArgumentException("JPAY 포털 ID를 입력하세요.");
        }
        String pgCd = trim(body.get("pgCd")).toUpperCase(Locale.ROOT);
        if (!pgCd.isEmpty() && !PgVendor.isJpayFamily(pgCd)) {
            throw new IllegalArgumentException("JPAY 계열 PG코드만 선택할 수 있습니다.");
        }

        long sameMasterCount = repository.countByMasterOrgUnitId(masterOrgUnitId);
        if (id != null && entity.getMasterOrgUnitId() != null && entity.getMasterOrgUnitId().equals(masterOrgUnitId)) {
            sameMasterCount = Math.max(0, sameMasterCount - 1);
        }
        if (pgCd.isEmpty() && sameMasterCount > 0) {
            throw new IllegalArgumentException("동일 총판에 추가 계정은 PG코드(JPY/USD 등)를 선택하세요.");
        }
        if (!pgCd.isEmpty()) {
            Optional<JpayPortalAccount> dupPg = repository.findByMasterOrgUnitIdAndPgCd(masterOrgUnitId, pgCd);
            if (dupPg.isPresent() && (id == null || !dupPg.get().getId().equals(id))) {
                throw new IllegalArgumentException("이 총판·PG코드 조합이 이미 등록되어 있습니다.");
            }
        }
        Optional<JpayPortalAccount> dupUser = repository.findByMasterOrgUnitIdAndPortalUsername(masterOrgUnitId, username);
        if (dupUser.isPresent() && (id == null || !dupUser.get().getId().equals(id))) {
            throw new IllegalArgumentException("이 총판에 동일 포털 ID가 이미 등록되어 있습니다.");
        }

        String newPw = trim(body.get("portalPassword"));
        if (newPw.isEmpty() && (entity.getPortalPassword() == null || entity.getPortalPassword().isBlank())) {
            throw new IllegalArgumentException("JPAY 포털 비밀번호를 입력하세요.");
        }
        if (!newPw.isEmpty()) {
            entity.setPortalPassword(newPw);
        }

        entity.setMasterOrgUnitId(masterOrgUnitId);
        entity.setMasterCompCode(masterCode);
        entity.setPortalUsername(username);
        entity.setLabel(trim(body.get("label")));
        entity.setPgCd(pgCd.isEmpty() ? null : pgCd);
        entity.setUseYn("Y".equalsIgnoreCase(trim(body.get("useYn"))) ? "Y" : "N");
        Integer sort = parseInt(body.get("sortOrder"));
        entity.setSortOrder(sort != null ? sort : 0);

        repository.save(entity);
        return toAdminMap(entity);
    }

    @Transactional
    public void delete(long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("계정을 찾을 수 없습니다.");
        }
        repository.deleteById(id);
    }

    private List<Map<String, Object>> listMasterDistOptions() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (OrgUnit ou : orgUnitRepository.findByOrgLevelOrderByCodeAsc(OrgLevel.MASTER_DIST)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orgUnitId", ou.getId());
            m.put("compCode", ou.getCode());
            m.put("compName", ou.getName());
            out.add(m);
        }
        return out;
    }

    private List<Map<String, Object>> listJpayPgOptions() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (PgAgency a : pgAgencyRepository.findAllByOrderByPgCdAsc()) {
            if (a.getPgCd() == null || !PgVendor.isJpayFamily(a.getPgCd())) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("pgCd", a.getPgCd().trim());
            m.put("pgNm", a.getPgNm());
            out.add(m);
        }
        return out;
    }

    private static Map<String, Object> toAdminMap(JpayPortalAccount a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("masterOrgUnitId", a.getMasterOrgUnitId());
        m.put("masterCompCode", a.getMasterCompCode());
        m.put("label", a.getLabel() != null ? a.getLabel() : "");
        m.put("pgCd", a.getPgCd() != null ? a.getPgCd() : "");
        m.put("portalUsername", a.getPortalUsername());
        m.put("portalPasswordSet", a.getPortalPassword() != null && !a.getPortalPassword().isBlank());
        m.put("useYn", a.getUseYn());
        m.put("sortOrder", a.getSortOrder());
        if (a.getUpdatedAt() != null) {
            m.put("updatedAt", a.getUpdatedAt().toString());
        }
        return m;
    }

    private static String trim(Object o) {
        return o != null ? o.toString().trim() : "";
    }

    private static Long parseLong(Object o) {
        if (o == null || o.toString().isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseInt(Object o) {
        if (o == null || o.toString().isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
