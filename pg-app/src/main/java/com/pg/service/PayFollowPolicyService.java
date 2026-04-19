package com.pg.service;

import com.pg.entity.AppUser;
import com.pg.entity.HqLedgerSysSettings;
import com.pg.entity.HqNotifyEnvConfig;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgLevelPayFollowCap;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgLevelPayFollowCapRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 결제 후속조치: 전산설정(전역) + 조직 단계 상한 + 가맹점(MERCHANT 로그인)별 세부.
 */
@Service
public class PayFollowPolicyService {

    /** 자동무효 시작(분) 미설정 시 기본 0:00(당일 자정) */
    private static final int DEFAULT_AUTO_VOID_START_MIN = 0;
    /** 자동무효 마감(분) 미설정 시 기본 21:00 — 태국·기준 Zone 당일 (일본 표시는 동일 시각 +2h → 23:00) */
    private static final int DEFAULT_AUTO_VOID_END_MIN = 21 * 60;
    private static final int EMAIL_VOID_END_MIN_FIXED = 23 * 60 + 59;
    /** 환불 가능 일수: DB NULL 시 7일 */
    private static final int DEFAULT_AUTO_REFUND_DAYS = 7;
    /** 환불·강제환불 일수(결제 익일 0시 앵커)는 태국 자정 기준으로만 계산 */
    private static final ZoneId REFUND_DAY_ZONE = ZoneId.of("Asia/Bangkok");

    private final HqNotifyEnvService hqNotifyEnvService;
    private final HqLedgerSysSettingsRepository ledgerSysSettingsRepository;
    private final OrgLevelPayFollowCapRepository capRepository;
    private final AuthService authService;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final PgTrnsctnRepository trnsctnRepository;
    private final OrgAccessService orgAccessService;

    public PayFollowPolicyService(HqNotifyEnvService hqNotifyEnvService,
                                  HqLedgerSysSettingsRepository ledgerSysSettingsRepository,
                                  OrgLevelPayFollowCapRepository capRepository,
                                  AuthService authService,
                                  OrgUnitRepository orgUnitRepository,
                                  MerchantProfileRepository merchantProfileRepository,
                                  PgTrnsctnRepository trnsctnRepository,
                                  OrgAccessService orgAccessService) {
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.ledgerSysSettingsRepository = ledgerSysSettingsRepository;
        this.capRepository = capRepository;
        this.authService = authService;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.trnsctnRepository = trnsctnRepository;
        this.orgAccessService = orgAccessService;
    }

    /** 본사권한설정 화면용: 단계별 Y/N (항상 7단계 키) */
    public Map<String, Map<String, Object>> buildLevelCapsPayload() {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (OrgLevel lv : OrgLevel.values()) {
            String code = lv.name();
            Optional<OrgLevelPayFollowCap> opt = capRepository.findById(code);
            OrgLevelPayFollowCap row = opt.orElseGet(() -> defaultCapRow(code));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("autoVoid", yn(row.getAutoVoidYn()));
            m.put("emailVoid", yn(row.getEmailVoidYn()));
            m.put("autoRefund", yn(row.getAutoRefundYn()));
            m.put("forceRefund", yn(row.getForceRefundYn()));
            out.put(code, m);
        }
        return out;
    }

    private static OrgLevelPayFollowCap defaultCapRow(String code) {
        OrgLevelPayFollowCap c = new OrgLevelPayFollowCap();
        c.setOrgLevel(code);
        c.setAutoVoidYn("Y");
        c.setEmailVoidYn("Y");
        c.setAutoRefundYn("Y");
        c.setForceRefundYn("Y");
        return c;
    }

    @Transactional
    public void saveLevelCapsFromClient(Map<?, ?> raw) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        for (OrgLevel lv : OrgLevel.values()) {
            String code = lv.name();
            Object node = raw.get(code);
            if (!(node instanceof Map<?, ?> sub)) {
                continue;
            }
            OrgLevelPayFollowCap row = capRepository.findById(code).orElseGet(() -> {
                OrgLevelPayFollowCap n = new OrgLevelPayFollowCap();
                n.setOrgLevel(code);
                return n;
            });
            row.setAutoVoidYn(boolToYn(sub.get("autoVoid")));
            row.setEmailVoidYn(boolToYn(sub.get("emailVoid")));
            row.setAutoRefundYn(boolToYn(sub.get("autoRefund")));
            row.setForceRefundYn(boolToYn(sub.get("forceRefund")));
            capRepository.save(row);
        }
    }

    private static String boolToYn(Object v) {
        if (v instanceof Boolean b) {
            return b ? "Y" : "N";
        }
        if (v == null) {
            return "N";
        }
        String s = v.toString().trim();
        if ("Y".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s)) {
            return "Y";
        }
        if ("N".equalsIgnoreCase(s) || "0".equals(s) || "no".equalsIgnoreCase(s)) {
            return "N";
        }
        if ("true".equalsIgnoreCase(s)) {
            return "Y";
        }
        if ("false".equalsIgnoreCase(s)) {
            return "N";
        }
        return "N";
    }

    /**
     * 결제 목록 meta: 현재 로그인 사용자 기준으로 버튼 노출.
     */
    public Map<String, Boolean> allowedActionsForViewer(AppUser user) {
        Map<String, Boolean> m = new LinkedHashMap<>();
        m.put("AUTO_VOID", false);
        m.put("EMAIL_VOID", false);
        m.put("AUTO_REFUND", false);
        m.put("FORCE_REFUND", false);
        if (user == null) {
            return m;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            HqNotifyEnvConfig env = hqNotifyEnvService.getOrCreate();
            m.put("AUTO_VOID", yn(env.getAutoVoidYn()));
            m.put("EMAIL_VOID", yn(env.getEmailVoidYn()));
            m.put("AUTO_REFUND", yn(env.getAutoRefundYn()));
            m.put("FORCE_REFUND", yn(env.getForceRefundYn()) && forceRefundPeriodPositive(env));
            return m;
        }
        Map<String, Object> org = authService.getOrgInfo(user.getUsername());
        if (org == null) {
            return m;
        }
        String level = String.valueOf(org.getOrDefault("orgLevel", "")).trim().toUpperCase(Locale.ROOT);
        HqNotifyEnvConfig env = hqNotifyEnvService.getOrCreate();
        OrgLevelPayFollowCap cap = capRepository.findById(level).orElse(null);

        boolean gVoid = yn(env.getAutoVoidYn()) && capAllows(cap, PayListActionService.PayFollowAction.AUTO_VOID);
        boolean gEmail = yn(env.getEmailVoidYn()) && capAllows(cap, PayListActionService.PayFollowAction.EMAIL_VOID);
        boolean gRef = yn(env.getAutoRefundYn()) && capAllows(cap, PayListActionService.PayFollowAction.AUTO_REFUND);
        boolean gForce = yn(env.getForceRefundYn()) && forceRefundPeriodPositive(env)
                && capAllows(cap, PayListActionService.PayFollowAction.FORCE_REFUND);

        if (!"MERCHANT".equals(level)) {
            m.put("AUTO_VOID", gVoid);
            m.put("EMAIL_VOID", gEmail);
            m.put("AUTO_REFUND", gRef);
            m.put("FORCE_REFUND", gForce);
            return m;
        }

        Object ouIdObj = org.get("orgUnitId");
        if (ouIdObj == null) {
            return m;
        }
        long ouId;
        try {
            ouId = Long.parseLong(ouIdObj.toString().trim());
        } catch (NumberFormatException e) {
            return m;
        }
        Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByOrgUnitId(ouId);
        if (mpOpt.isEmpty()) {
            return m;
        }
        MerchantProfile mp = mpOpt.get();
        if (!merchantPayFollowUseEffective(mp)) {
            return m;
        }
        m.put("AUTO_VOID", gVoid && merchantActionEffective(mp.getPayFollowAutoVoidYn()));
        m.put("EMAIL_VOID", gEmail && merchantActionEffective(mp.getPayFollowEmailVoidYn()));
        m.put("AUTO_REFUND", gRef && merchantActionEffective(mp.getPayFollowAutoRefundYn()));
        m.put("FORCE_REFUND", gForce && merchantActionEffective(mp.getPayFollowForceRefundYn()));
        return m;
    }

    /**
     * 거래 행별 후속조치 — 권한·환경설정 AND 설정된 시간/일자 창. 승인(10)만 true 가능.
     */
    public Map<String, Boolean> payFollowRowEnabled(AppUser viewer, PgTrnsctn t) {
        Map<String, Boolean> base = allowedActionsForViewer(viewer);
        Map<String, Boolean> out = new LinkedHashMap<>();
        out.put("AUTO_VOID", false);
        out.put("EMAIL_VOID", false);
        out.put("AUTO_REFUND", false);
        out.put("FORCE_REFUND", false);
        if (t == null || !"10".equals(t.getStatus())) {
            return out;
        }
        HqNotifyEnvConfig env = hqNotifyEnvService.getOrCreate();
        ZoneId ref = resolvePayFollowZone(env);
        out.put("AUTO_VOID", Boolean.TRUE.equals(base.get("AUTO_VOID")) && withinAutoVoidWindow(t, env, ref));
        out.put("EMAIL_VOID", Boolean.TRUE.equals(base.get("EMAIL_VOID")) && withinEmailVoidWindow(t, env, ref));
        out.put("AUTO_REFUND", Boolean.TRUE.equals(base.get("AUTO_REFUND")) && withinAutoRefundDays(t, env, REFUND_DAY_ZONE));
        out.put("FORCE_REFUND", Boolean.TRUE.equals(base.get("FORCE_REFUND")) && withinForceRefundDays(t, env, REFUND_DAY_ZONE));
        return out;
    }

    private ZoneId resolvePayFollowZone(HqNotifyEnvConfig env) {
        String z = env.getPayFollowRefZone();
        if (z == null || z.isBlank()) {
            Optional<HqLedgerSysSettings> ls = ledgerSysSettingsRepository.findFirstByOrderByIdAsc();
            if (ls.isPresent()) {
                String tz = ls.get().getDisplayTimezone();
                if (tz != null && !tz.isBlank()) {
                    z = tz.trim();
                }
            }
        } else {
            z = z.trim();
        }
        if (z == null || z.isBlank()) {
            z = "Asia/Bangkok";
        }
        try {
            return ZoneId.of(z);
        } catch (Exception e) {
            return ZoneId.of("Asia/Bangkok");
        }
    }

    private static LocalDateTime approvalLocalTime(PgTrnsctn t) {
        if (t.getPaidAt() != null) {
            return t.getPaidAt();
        }
        return t.getCreatedAt();
    }

    private static ZonedDateTime approvalAtZone(PgTrnsctn t, ZoneId ref) {
        LocalDateTime la = approvalLocalTime(t);
        if (la == null) {
            return null;
        }
        return la.atZone(ref);
    }

    private static boolean forceRefundPeriodPositive(HqNotifyEnvConfig env) {
        Integer f = env.getForceRefundAfterDays();
        return f != null && f > 0;
    }

    private static int resolvedAutoRefundDays(HqNotifyEnvConfig env) {
        Integer d = env.getAutoRefundAfterDays();
        if (d == null) {
            return DEFAULT_AUTO_REFUND_DAYS;
        }
        return d;
    }

    /**
     * 자동무효: 승인일(기준 Zone) <strong>당일</strong>이고, 현재 시각이 설정한 시작~마감(분) 구간 안.
     * 시작·마감 미설정 시 기본 <strong>0:00 ~ 21:00</strong>(마감은 태국과 동일 시각이면 일본에서 +2h인 23:00에 해당).
     */
    private static boolean withinAutoVoidWindow(PgTrnsctn t, HqNotifyEnvConfig env, ZoneId ref) {
        ZonedDateTime now = ZonedDateTime.now(ref);
        ZonedDateTime approval = approvalAtZone(t, ref);
        if (approval == null) {
            return false;
        }
        if (!now.toLocalDate().equals(approval.toLocalDate())) {
            return false;
        }
        int startMin = env.getAutoVoidStartMin() != null ? env.getAutoVoidStartMin() : DEFAULT_AUTO_VOID_START_MIN;
        int endMin = env.getAutoVoidEndMin() != null ? env.getAutoVoidEndMin() : DEFAULT_AUTO_VOID_END_MIN;
        int nm = now.getHour() * 60 + now.getMinute();
        return nm >= startMin && nm <= endMin;
    }

    /**
     * 수동(이메일)무효: 승인일 당일, 설정한 시작~마감(분). 마감 NULL이면 23:59. 자동무효와 겹치면 시작은 마감 직후로 올림.
     */
    private static boolean withinEmailVoidWindow(PgTrnsctn t, HqNotifyEnvConfig env, ZoneId ref) {
        ZonedDateTime now = ZonedDateTime.now(ref);
        ZonedDateTime approval = approvalAtZone(t, ref);
        if (approval == null) {
            return false;
        }
        if (!now.toLocalDate().equals(approval.toLocalDate())) {
            return false;
        }
        int effEnd = resolvedEmailVoidEndMin(env);
        Integer es = env.getEmailVoidStartMin();
        Integer ae = env.getAutoVoidEndMin();
        int autoEndEff = ae != null ? ae : DEFAULT_AUTO_VOID_END_MIN;
        boolean autoVoidOn = yn(env.getAutoVoidYn());
        int effStart;
        if (autoVoidOn && autoEndEff < EMAIL_VOID_END_MIN_FIXED) {
            int afterAuto = autoEndEff + 1;
            effStart = es != null ? Math.max(es, afterAuto) : afterAuto;
        } else if (es != null) {
            effStart = es;
        } else if (ae != null && ae < EMAIL_VOID_END_MIN_FIXED) {
            effStart = ae + 1;
        } else {
            effStart = 0;
        }
        if (effStart < 0 || effStart > effEnd) {
            return false;
        }
        int nm = now.getHour() * 60 + now.getMinute();
        return nm >= effStart && nm <= effEnd;
    }

    private static int resolvedEmailVoidEndMin(HqNotifyEnvConfig env) {
        Integer e = env.getEmailVoidEndMin();
        return e != null ? e : EMAIL_VOID_END_MIN_FIXED;
    }

    /**
     * 결제일(paidAt, 없으면 생성시각)을 {@link #REFUND_DAY_ZONE}(태국) 달력으로 본 뒤, 그 익일 0시부터 N일.
     * N이 DB NULL이면 7일. N≤0이면 익일 0시 이후 제한 없음.
     */
    private static boolean withinAutoRefundDays(PgTrnsctn t, HqNotifyEnvConfig env, ZoneId ref) {
        int d = resolvedAutoRefundDays(env);
        if (d <= 0) {
            return withinRefundOpenEnded(t, env, ref);
        }
        return withinRefundStyleWindow(t, env, ref, d);
    }

    /**
     * 일반 환불 가능 기간이 끝난 <strong>다음날 0시</strong>부터 M일(M&gt;0). M이 0·NULL이면 허용 안 함.
     */
    private static boolean withinForceRefundDays(PgTrnsctn t, HqNotifyEnvConfig env, ZoneId ref) {
        Integer m = env.getForceRefundAfterDays();
        if (m == null || m <= 0) {
            return false;
        }
        ZonedDateTime payZ = approvalAtZone(t, ref);
        if (payZ == null) {
            return false;
        }
        LocalDate payLocal = payZ.toLocalDate();
        int n = resolvedAutoRefundDays(env);
        ZonedDateTime normalStart = refundWindowStartZdt(payLocal, ref, env);
        ZonedDateTime now = ZonedDateTime.now(ref);
        if (n <= 0) {
            return false;
        }
        ZonedDateTime normalEndExclusive = normalStart.plusDays(n);
        ZonedDateTime forceStart = normalEndExclusive;
        ZonedDateTime forceEndExclusive = forceStart.plusDays(m);
        return !now.isBefore(forceStart) && now.isBefore(forceEndExclusive);
    }

    private static boolean withinRefundOpenEnded(PgTrnsctn t, HqNotifyEnvConfig env, ZoneId ref) {
        ZonedDateTime az = approvalAtZone(t, ref);
        if (az == null) {
            return false;
        }
        LocalDate paidDate = az.toLocalDate();
        ZonedDateTime windowStart = refundWindowStartZdt(paidDate, ref, env);
        ZonedDateTime now = ZonedDateTime.now(ref);
        return !now.isBefore(windowStart);
    }

    private static boolean withinRefundStyleWindow(PgTrnsctn t, HqNotifyEnvConfig env, ZoneId ref, int days) {
        ZonedDateTime az = approvalAtZone(t, ref);
        if (az == null) {
            return false;
        }
        LocalDate paidDate = az.toLocalDate();
        ZonedDateTime windowStart = refundWindowStartZdt(paidDate, ref, env);
        ZonedDateTime windowEndExclusive = windowStart.plusDays(days);
        ZonedDateTime now = ZonedDateTime.now(ref);
        return !now.isBefore(windowStart) && now.isBefore(windowEndExclusive);
    }

    private static int resolvedRefundWindowStartMin(HqNotifyEnvConfig env) {
        Integer v = env.getAutoRefundWindowStartMin();
        if (v == null || v < 0) {
            return 0;
        }
        return Math.min(v, EMAIL_VOID_END_MIN_FIXED);
    }

    /** 결제일(태국) 익일, 설정한 시각(분)부터. NULL이면 0:00. */
    private static ZonedDateTime refundWindowStartZdt(LocalDate paidDate, ZoneId zone, HqNotifyEnvConfig env) {
        int sm = resolvedRefundWindowStartMin(env);
        LocalDate dayAfter = paidDate.plusDays(1);
        LocalTime t = LocalTime.ofSecondOfDay(sm * 60L);
        return ZonedDateTime.of(dayAfter, t, zone);
    }

    /** 가맹점 관리자: NULL 컬럼은 기존 호환(허용). 명시 N 만 거부. */
    private static boolean merchantPayFollowUseEffective(MerchantProfile mp) {
        String u = mp.getPayFollowMerchantUseYn();
        if (u == null || u.isBlank()) {
            return true;
        }
        return yn(u);
    }

    private static boolean merchantActionEffective(String col) {
        if (col == null || col.isBlank()) {
            return true;
        }
        return yn(col);
    }

    private static boolean capAllows(OrgLevelPayFollowCap cap, PayListActionService.PayFollowAction a) {
        if (cap == null) {
            return true;
        }
        return switch (a) {
            case AUTO_VOID -> yn(cap.getAutoVoidYn());
            case EMAIL_VOID -> yn(cap.getEmailVoidYn());
            case AUTO_REFUND -> yn(cap.getAutoRefundYn());
            case FORCE_REFUND -> yn(cap.getForceRefundYn());
        };
    }

    public void assertMayExecute(AppUser user, String trnId, PayListActionService.PayFollowAction action) {
        HqNotifyEnvConfig env = hqNotifyEnvService.getOrCreate();
        switch (action) {
            case AUTO_VOID -> requireYn(env.getAutoVoidYn(), "자동무효");
            case EMAIL_VOID -> requireYn(env.getEmailVoidYn(), "이메일무효");
            case AUTO_REFUND -> requireYn(env.getAutoRefundYn(), "자동환불");
            case FORCE_REFUND -> requireYn(env.getForceRefundYn(), "강제환불");
        }
        if (user == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        PgTrnsctn t = trnsctnRepository.findById(trnId.trim())
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다."));
        if (!"10".equals(t.getStatus())) {
            throw new IllegalStateException("승인(결제) 완료 건만 후속조치할 수 있습니다.");
        }
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            Map<String, Object> org = authService.getOrgInfo(user.getUsername());
            if (org == null) {
                throw new IllegalStateException("조직 정보를 확인할 수 없습니다.");
            }
            String level = String.valueOf(org.getOrDefault("orgLevel", "")).trim().toUpperCase(Locale.ROOT);
            OrgLevelPayFollowCap cap = capRepository.findById(level).orElse(null);
            if (!capAllows(cap, action)) {
                throw new IllegalStateException("본사권한설정의 조직 단계별 결제 후속조치 상한에서 이 기능이 꺼져 있습니다.");
            }
            if ("MERCHANT".equals(level)) {
                String myComp = org.get("compId") != null ? org.get("compId").toString().trim() : "";
                if (myComp.isEmpty() || !myComp.equalsIgnoreCase(String.valueOf(t.getMerchantId()).trim())) {
                    throw new IllegalStateException("소속 가맹점 거래만 후속조치할 수 있습니다.");
                }
                Object ouIdObj = org.get("orgUnitId");
                if (ouIdObj == null) {
                    throw new IllegalStateException("가맹점 조직 정보가 없습니다.");
                }
                long ouId = Long.parseLong(ouIdObj.toString().trim());
                MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(ouId)
                        .orElseThrow(() -> new IllegalStateException("가맹점 프로필을 찾을 수 없습니다."));
                if (!merchantPayFollowUseEffective(mp)) {
                    throw new IllegalStateException("가맹점 정보에서 결제 후속조치 사용이 꺼져 있습니다.");
                }
                boolean ok = switch (action) {
                    case AUTO_VOID -> merchantActionEffective(mp.getPayFollowAutoVoidYn());
                    case EMAIL_VOID -> merchantActionEffective(mp.getPayFollowEmailVoidYn());
                    case AUTO_REFUND -> merchantActionEffective(mp.getPayFollowAutoRefundYn());
                    case FORCE_REFUND -> merchantActionEffective(mp.getPayFollowForceRefundYn());
                };
                if (!ok) {
                    throw new IllegalStateException("가맹점 정보에서 해당 후속조치가 허용되지 않습니다.");
                }
            } else {
                String viewerComp = org.get("compId") != null ? org.get("compId").toString().trim() : "";
                String merchantId = t.getMerchantId() != null ? t.getMerchantId().trim() : "";
                if (viewerComp.isEmpty() || merchantId.isEmpty()
                        || !orgAccessService.isTargetUnderViewerOrg(viewerComp, merchantId)) {
                    throw new IllegalStateException("소속 업체 및 하위 가맹점 거래만 후속조치할 수 있습니다.");
                }
            }
        }
        ZoneId ref = resolvePayFollowZone(env);
        switch (action) {
            case AUTO_VOID -> {
                if (!withinAutoVoidWindow(t, env, ref)) {
                    throw new IllegalStateException(
                            "자동무효는 승인일(시간 선택 국가 기준) 당일, 설정한 시작~마감 시각 안에서만 가능합니다.");
                }
            }
            case EMAIL_VOID -> {
                if (!withinEmailVoidWindow(t, env, ref)) {
                    throw new IllegalStateException(
                            "이메일무효는 승인일(기준 Zone) 당일, 설정한 시작~마감 시각 안에서만 가능합니다.");
                }
            }
            case AUTO_REFUND -> {
                if (!withinAutoRefundDays(t, env, REFUND_DAY_ZONE)) {
                    throw new IllegalStateException(
                            "자동환불 처리 가능 기간이 아닙니다. (태국 기준 결제일 익일 설정 시각부터 환불 가능 일수 확인)");
                }
            }
            case FORCE_REFUND -> {
                if (!withinForceRefundDays(t, env, REFUND_DAY_ZONE)) {
                    throw new IllegalStateException(
                            "강제환불 처리 가능 기간이 아닙니다. (태국 기준 일반 환불 종료 시점 다음날 동일 시각부터 강제환불 일수 확인)");
                }
            }
        }
    }

    private static void requireYn(String yn, String label) {
        if (!PayFollowPolicyService.yn(yn)) {
            throw new IllegalStateException("본사설정 > 전산설정관리에서 [" + label + "] 사용이 꺼져 있습니다.");
        }
    }

    private static boolean yn(String s) {
        return s != null && "Y".equalsIgnoreCase(s.trim());
    }

    /**
     * 상위 조직이 가맹점 후속 플래그를 저장할 때 — 조직 단계(MERCHANT) 상한 및 개별 액션 상한 위로 올리지 못함.
     */
    public void clampMerchantPayFollowToLevelCeiling(MerchantProfile mp) {
        if (mp == null) {
            return;
        }
        OrgLevelPayFollowCap cap = capRepository.findById("MERCHANT").orElse(null);
        if (cap == null) {
            return;
        }
        if (!yn(cap.getAutoVoidYn()) && mp.getPayFollowAutoVoidYn() != null && yn(mp.getPayFollowAutoVoidYn())) {
            mp.setPayFollowAutoVoidYn("N");
        }
        if (!yn(cap.getEmailVoidYn()) && mp.getPayFollowEmailVoidYn() != null && yn(mp.getPayFollowEmailVoidYn())) {
            mp.setPayFollowEmailVoidYn("N");
        }
        if (!yn(cap.getAutoRefundYn()) && mp.getPayFollowAutoRefundYn() != null && yn(mp.getPayFollowAutoRefundYn())) {
            mp.setPayFollowAutoRefundYn("N");
        }
        if (!yn(cap.getForceRefundYn()) && mp.getPayFollowForceRefundYn() != null && yn(mp.getPayFollowForceRefundYn())) {
            mp.setPayFollowForceRefundYn("N");
        }
        if (!yn(cap.getAutoVoidYn()) && !yn(cap.getEmailVoidYn()) && !yn(cap.getAutoRefundYn()) && !yn(cap.getForceRefundYn())) {
            mp.setPayFollowMerchantUseYn("N");
        }
    }
}
