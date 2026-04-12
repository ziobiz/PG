package com.pg.service;

import com.pg.entity.HqNotifyTarget;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.HqNotifyTargetRepository;
import com.pg.repository.OrgUnitRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class HqNotifyTargetService {
    private static final String CH_CALLBACK = "CALLBACK";
    private static final String CH_RESULT = "RESULT";
    private static final String PREFIX_CB = "cb";
    private static final String PREFIX_RS = "rs";
    private static final char[] URL_SAFE = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final int SUFFIX_LEN = 6;
    private static final SecureRandom RND = new SecureRandom();

    private final HqNotifyTargetRepository repository;
    private final HqNotifyEnvService hqNotifyEnvService;
    private final OrgUnitRepository orgUnitRepository;

    public HqNotifyTargetService(HqNotifyTargetRepository repository, HqNotifyEnvService hqNotifyEnvService,
                                 OrgUnitRepository orgUnitRepository) {
        this.repository = repository;
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.orgUnitRepository = orgUnitRepository;
    }

    public List<Map<String, Object>> list() {
        return repository.findAllByOrderByIdDesc().stream().map(this::toMap).toList();
    }

    /** 노티 자동생성 시 선택용: 총판(MASTER_DIST) 조직 목록 */
    public List<Map<String, Object>> listMasterDistNotifyLinkOptions() {
        return orgUnitRepository.findByOrgLevelOrderByCodeAsc(OrgLevel.MASTER_DIST).stream()
                .filter(o -> o.getStatus() == null || "ACTIVE".equalsIgnoreCase(o.getStatus()))
                .map(o -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", o.getId());
                    m.put("code", o.getCode() != null ? o.getCode() : "");
                    m.put("name", o.getName() != null ? o.getName() : "");
                    return m;
                })
                .toList();
    }

    /**
     * 동일 대상명으로 CALLBACK·RESULT 노티 URL을 각각 한 건씩 생성합니다.
     * URL 경로 코드는 cb/rs + 6자(영숫자)로 짧게 부여합니다.
     *
     * @param boundOrgUnitId 연결 총판(tb_org_unit.id, MASTER_DIST 필수)
     */
    @Transactional
    public Map<String, Object> createPair(String targetName, Long boundOrgUnitId, HttpServletRequest req) {
        String name = targetName == null ? "" : targetName.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("노티 대상 이름을 입력하세요.");
        if (boundOrgUnitId == null) {
            throw new IllegalArgumentException("연결 총판을 선택하세요.");
        }
        OrgUnit bound = orgUnitRepository.findById(boundOrgUnitId)
                .orElseThrow(() -> new IllegalArgumentException("연결 총판 조직을 찾을 수 없습니다."));
        if (bound.getOrgLevel() != OrgLevel.MASTER_DIST) {
            throw new IllegalArgumentException("연결 총판은 총판(MASTER_DIST)만 선택할 수 있습니다.");
        }
        if (bound.getStatus() != null && !"ACTIVE".equalsIgnoreCase(bound.getStatus())) {
            throw new IllegalArgumentException("미사용 조직은 연결 총판으로 선택할 수 없습니다.");
        }
        var cfg = hqNotifyEnvService.getOrCreate();
        String ingressBase = hqNotifyEnvService.buildNotifyIngressUrl(cfg, req);

        String cbCode;
        String rsCode;
        int guard = 0;
        do {
            String suf = randomSuffix(SUFFIX_LEN);
            cbCode = PREFIX_CB + suf;
            rsCode = PREFIX_RS + suf;
            guard++;
            if (guard > 200) {
                throw new IllegalStateException("노티 코드 생성에 실패했습니다. 잠시 후 다시 시도하세요.");
            }
        } while (repository.findByTargetCode(cbCode).isPresent() || repository.findByTargetCode(rsCode).isPresent());

        HqNotifyTarget tCb = new HqNotifyTarget();
        tCb.setTargetCode(cbCode);
        tCb.setTargetName(name);
        tCb.setTargetUrl(ingressBase + "/" + cbCode);
        tCb.setChannelType(CH_CALLBACK);
        tCb.setUseYn("Y");
        tCb.setOrgUnitId(boundOrgUnitId);
        repository.save(tCb);

        HqNotifyTarget tRs = new HqNotifyTarget();
        tRs.setTargetCode(rsCode);
        tRs.setTargetName(name);
        tRs.setTargetUrl(ingressBase + "/" + rsCode);
        tRs.setChannelType(CH_RESULT);
        tRs.setUseYn("Y");
        tRs.setOrgUnitId(boundOrgUnitId);
        repository.save(tRs);

        Map<String, Object> out = new LinkedHashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(toMap(tCb));
        rows.add(toMap(tRs));
        out.put("targets", rows);
        out.put("callbackUrl", tCb.getTargetUrl());
        out.put("resultUrl", tRs.getTargetUrl());
        return out;
    }

    private static String randomSuffix(int len) {
        char[] buf = new char[len];
        for (int i = 0; i < len; i++) {
            buf[i] = URL_SAFE[RND.nextInt(URL_SAFE.length)];
        }
        return new String(buf);
    }

    @Transactional
    public void delete(Long id) {
        HqNotifyTarget t = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("노티 대상을 찾을 수 없습니다."));
        repository.delete(t);
    }

    /**
     * 기존 노티 대상(한 건 또는 CALLBACK·RESULT 쌍)에 연결 총판을 지정·변경합니다.
     */
    @Transactional
    public void bindBoundOrgToTargets(List<Long> targetIds, Long boundOrgUnitId) {
        if (targetIds == null || targetIds.isEmpty()) {
            throw new IllegalArgumentException("대상 노티를 지정하세요.");
        }
        if (boundOrgUnitId == null) {
            throw new IllegalArgumentException("연결 총판을 선택하세요.");
        }
        OrgUnit bound = orgUnitRepository.findById(boundOrgUnitId)
                .orElseThrow(() -> new IllegalArgumentException("연결 총판 조직을 찾을 수 없습니다."));
        if (bound.getOrgLevel() != OrgLevel.MASTER_DIST) {
            throw new IllegalArgumentException("연결 총판은 총판(MASTER_DIST)만 선택할 수 있습니다.");
        }
        if (bound.getStatus() != null && !"ACTIVE".equalsIgnoreCase(bound.getStatus())) {
            throw new IllegalArgumentException("미사용 조직은 연결 총판으로 선택할 수 없습니다.");
        }
        List<HqNotifyTarget> rows = repository.findAllById(targetIds);
        if (rows.size() != targetIds.size()) {
            throw new IllegalArgumentException("노티 대상을 찾을 수 없습니다.");
        }
        if (rows.size() > 1) {
            validateSameNotifyPair(rows);
        }
        for (HqNotifyTarget t : rows) {
            t.setOrgUnitId(boundOrgUnitId);
            repository.save(t);
        }
    }

    private static void validateSameNotifyPair(List<HqNotifyTarget> rows) {
        String name0 = rows.get(0).getTargetName();
        for (int i = 1; i < rows.size(); i++) {
            if (!Objects.equals(name0, rows.get(i).getTargetName())) {
                throw new IllegalArgumentException("동일 노티 대상명의 CALLBACK·RESULT 쌍만 함께 연결할 수 있습니다.");
            }
        }
        if (rows.size() == 2) {
            String c1 = channelUpper(rows.get(0));
            String c2 = channelUpper(rows.get(1));
            boolean ok = (CH_CALLBACK.equals(c1) && CH_RESULT.equals(c2)) || (CH_CALLBACK.equals(c2) && CH_RESULT.equals(c1));
            if (!ok) {
                throw new IllegalArgumentException("CALLBACK·RESULT 한 쌍만 연결할 수 있습니다.");
            }
            String su1 = pairSuffix(rows.get(0).getTargetCode());
            String su2 = pairSuffix(rows.get(1).getTargetCode());
            if (su1.isEmpty() || !su1.equals(su2)) {
                throw new IllegalArgumentException("동일 경로 접미(cb·rs 이후)의 쌍만 연결할 수 있습니다.");
            }
        } else if (rows.size() > 2) {
            throw new IllegalArgumentException("한 번에 연결할 노티 대상은 2건 이하입니다.");
        }
    }

    private static String channelUpper(HqNotifyTarget t) {
        if (t.getChannelType() == null) {
            return "";
        }
        return t.getChannelType().trim().toUpperCase(Locale.ROOT);
    }

    private static String pairSuffix(String code) {
        if (code == null) {
            return "";
        }
        String c = code.trim();
        if (c.length() > 2 && (c.regionMatches(true, 0, PREFIX_CB, 0, 2) || c.regionMatches(true, 0, PREFIX_RS, 0, 2))) {
            return c.substring(2).toLowerCase(Locale.ROOT);
        }
        return c.toLowerCase(Locale.ROOT);
    }

    /**
     * 총판 NOTIFY_1~4 저장 시, URL 문자열이 본사 발급 {@code tb_hq_notify_target.target_url} 과 일치하면 해당 행에 조직을 연결합니다.
     * 기존에 이 총판({@code orgUnitId})에만 연결돼 있던 행은 먼저 해제한 뒤 다시 매칭합니다.
     */
    @Transactional
    public void replaceDistributorOrgLinks(Long orgUnitId, List<String> notifyUrls) {
        if (orgUnitId == null) {
            return;
        }
        repository.clearOrgUnitIdByOrgUnitId(orgUnitId);
        repository.flush();
        if (notifyUrls == null) {
            return;
        }
        for (String raw : notifyUrls) {
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            String u = raw.trim();
            repository.findByTargetUrl(u).ifPresent(t -> {
                t.setOrgUnitId(orgUnitId);
                repository.save(t);
            });
        }
    }

    private Map<String, Object> toMap(HqNotifyTarget t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("targetCode", t.getTargetCode());
        m.put("targetName", t.getTargetName());
        m.put("targetUrl", t.getTargetUrl());
        String ch = t.getChannelType() != null ? t.getChannelType().trim().toUpperCase(Locale.ROOT) : CH_CALLBACK;
        m.put("channelType", ch);
        m.put("channelTypeNm", channelLabelKo(ch));
        m.put("useYn", t.getUseYn());
        m.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
        m.put("boundOrgUnitId", null);
        m.put("boundOrgUnitCode", "");
        m.put("boundOrgUnitName", "");
        if (t.getOrgUnitId() != null) {
            orgUnitRepository.findById(t.getOrgUnitId()).ifPresent(ou -> {
                m.put("boundOrgUnitId", ou.getId());
                m.put("boundOrgUnitCode", ou.getCode() != null ? ou.getCode() : "");
                m.put("boundOrgUnitName", ou.getName() != null ? ou.getName() : "");
            });
        }
        return m;
    }

    private static String channelLabelKo(String ch) {
        if (CH_RESULT.equals(ch)) return "RESULT (브라우저·리다이렉트)";
        return "CALLBACK (서버 노티)";
    }
}
