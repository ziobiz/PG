package com.pg.service;

import com.pg.entity.AppUser;
import com.pg.entity.HqLedgerSysSettings;
import com.pg.entity.HqNotifyEnvConfig;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgLevelPayFollowCap;
import com.pg.entity.PgAgency;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgLevelPayFollowCapRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.PgAgencyPayFollowCapability;
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
 * 결제 후속조치: 대행사별 허용 AND 전산설정(전역) AND 조직 단계 상한 AND 가맹점(MERCHANT) AND 계열·시간 창.
 * 노티 미들웨어는 거래 적재만 담당하며, 후속조치 스위치와 독립이다.
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
    private final PgAgencyRepository pgAgencyRepository;

    public PayFollowPolicyService(HqNotifyEnvService hqNotifyEnvService,
                                  HqLedgerSysSettingsRepository ledgerSysSettingsRepository,
                                  OrgLevelPayFollowCapRepository capRepository,
                                  AuthService authService,
                                  OrgUnitRepository orgUnitRepository,
                                  MerchantProfileRepository merchantProfileRepository,
                                  PgTrnsctnRepository trnsctnRepository,
                                  OrgAccessService orgAccessService,
                                  PgAgencyRepository pgAgencyRepository) {
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.ledgerSysSettingsRepository = ledgerSysSettingsRepository;
        this.capRepository = capRepository;
        this.authService = authService;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.trnsctnRepository = trnsctnRepository;
        this.orgAccessService = orgAccessService;
        this.pgAgencyRepository = pgAgencyRepository;
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
            m.put("manualVoid", yn(row.getManualVoidYn()));
            m.put("autoRefund", yn(row.getAutoRefundYn()));
            m.put("manualRefund", yn(row.getManualRefundYn()));
            m.put("forceRefund", yn(row.getForceRefundYn()));
            m.put("sameDayRefund", yn(row.getSameDayRefundYn()));
            out.put(code, m);
        }
        return out;
    }

    private static OrgLevelPayFollowCap defaultCapRow(String code) {
        OrgLevelPayFollowCap c = new OrgLevelPayFollowCap();
        c.setOrgLevel(code);
        c.setAutoVoidYn("Y");
        c.setEmailVoidYn("Y");
        c.setManualVoidYn("Y");
        c.setAutoRefundYn("Y");
        c.setManualRefundYn("Y");
        c.setForceRefundYn("Y");
        c.setSameDayRefundYn("N");
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
            row.setManualVoidYn(boolToYn(sub.get("manualVoid")));
            row.setAutoRefundYn(boolToYn(sub.get("autoRefund")));
            row.setManualRefundYn(boolToYn(sub.get("manualRefund")));
            row.setForceRefundYn(boolToYn(sub.get("forceRefund")));
            if (sub.containsKey("sameDayRefund")) {
                row.setSameDayRefundYn(boolToYn(sub.get("sameDayRefund")));
            } else if (row.getSameDayRefundYn() == null || row.getSameDayRefundYn().isBlank()) {
                row.setSameDayRefundYn("N");
            }
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
        m.put("MANUAL_VOID", false);
        m.put("MANUAL_REFUND", false);
        if (user == null) {
            return m;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            HqNotifyEnvConfig env = hqNotifyEnvService.getOrCreate();
            m.put("AUTO_VOID", yn(env.getAutoVoidYn()));
            m.put("EMAIL_VOID", yn(env.getEmailVoidYn()));
            m.put("AUTO_REFUND", yn(env.getAutoRefundYn()));
            m.put("FORCE_REFUND", yn(env.getForceRefundYn()) && forceRefundPeriodPositive(env));
            m.put("MANUAL_VOID", yn(env.getManualVoidYn()));
            m.put("MANUAL_REFUND", yn(env.getManualRefundYn()));
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
        boolean gManualVoid = yn(env.getManualVoidYn()) && capAllows(cap, PayListActionService.PayFollowAction.MANUAL_VOID);
        boolean gManualRefund = yn(env.getManualRefundYn()) && capAllows(cap, PayListActionService.PayFollowAction.MANUAL_REFUND);

        if (!"MERCHANT".equals(level)) {
            m.put("AUTO_VOID", gVoid);
            m.put("EMAIL_VOID", gEmail);
            m.put("AUTO_REFUND", gRef);
            m.put("FORCE_REFUND", gForce);
            m.put("MANUAL_VOID", gManualVoid);
            m.put("MANUAL_REFUND", gManualRefund);
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
        m.put("MANUAL_VOID", gManualVoid && merchantActionEffective(mp.getPayFollowManualVoidYn()));
        m.put("MANUAL_REFUND", gManualRefund && merchantActionEffective(mp.getPayFollowManualRefundYn()));
        return m;
    }

    /** JPAY — 자동환불·강제환불 API + 수동무효·수동환불 */
    public static boolean isJpayManualFollowTransaction(PgTrnsctn t) {
        return t != null && PgVendor.isJpayFamily(t.getVan());
    }

    /** 이메일무효 — ChillPay만. JPAY·URL(ElementPay)·Eximbay 등은 미지원. */
    public static boolean isEmailVoidFollowTransaction(PgTrnsctn t) {
        return t != null && PgVendor.isChillPayFamily(t.getVan());
    }

    /** ElementPay·Eximbay — 환불 API만(자동무효·이메일무효 없음). 캐비닛 환불은 웹훅으로 반영. */
    public static boolean isRefundApiFollowTransaction(PgTrnsctn t) {
        return t != null && (PgVendor.isElementPayFamily(t.getVan()) || PgVendor.isEximbayFamily(t.getVan()));
    }

    /**
     * 후속조치 열 자체를 숨김(성공내역 등). JPAY는 {@link #isJpayManualFollowTransaction} 으로 수동 2종 표시.
     */
    public static boolean isPayFollowHiddenForTransaction(PgTrnsctn t) {
        return false;
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
        out.put("MANUAL_VOID", false);
        out.put("MANUAL_REFUND", false);
        if (t == null || !"10".equals(t.getStatus())) {
            return out;
        }
        if (isJpayManualFollowTransaction(t)) {
            HqNotifyEnvConfig env = hqNotifyEnvService.getOrCreate();
            out.put("AUTO_REFUND", Boolean.TRUE.equals(base.get("AUTO_REFUND"))
                    && agencyAllows(t, PayListActionService.PayFollowAction.AUTO_REFUND)
                    && withinAutoRefundDays(t, env, REFUND_DAY_ZONE));
            out.put("FORCE_REFUND", Boolean.TRUE.equals(base.get("FORCE_REFUND"))
                    && agencyAllows(t, PayListActionService.PayFollowAction.FORCE_REFUND)
                    && withinForceRefundDays(t, env, REFUND_DAY_ZONE));
            out.put("MANUAL_VOID", Boolean.TRUE.equals(base.get("MANUAL_VOID"))
                    && agencyAllows(t, PayListActionService.PayFollowAction.MANUAL_VOID));
            out.put("MANUAL_REFUND", Boolean.TRUE.equals(base.get("MANUAL_REFUND"))
                    && agencyAllows(t, PayListActionService.PayFollowAction.MANUAL_REFUND));
            return out;
        }
        /* ElementPay·Eximbay: 환불 API만 — 자동무효·이메일무효 비활성 */
        if (PgVendor.isElementPayFamily(t.getVan()) || PgVendor.isEximbayFamily(t.getVan())) {
            HqNotifyEnvConfig env = hqNotifyEnvService.getOrCreate();
            boolean refundWin = withinAutoRefundDays(t, env, REFUND_DAY_ZONE)
                    || sameDayRefundOpen(viewer, t, env);
            out.put("AUTO_REFUND", Boolean.TRUE.equals(base.get("AUTO_REFUND"))
                    && agencyAllows(t, PayListActionService.PayFollowAction.AUTO_REFUND)
                    && refundWin);
            out.put("FORCE_REFUND", Boolean.TRUE.equals(base.get("FORCE_REFUND"))
                    && agencyAllows(t, PayListActionService.PayFollowAction.FORCE_REFUND)
                    && withinForceRefundDays(t, env, REFUND_DAY_ZONE));
            return out;
        }
        HqNotifyEnvConfig env = hqNotifyEnvService.getOrCreate();
        ZoneId ref = resolvePayFollowZone(env);
        out.put("AUTO_VOID", Boolean.TRUE.equals(base.get("AUTO_VOID"))
                && agencyAllows(t, PayListActionService.PayFollowAction.AUTO_VOID)
                && withinAutoVoidWindow(t, env, ref));
        out.put("EMAIL_VOID", isEmailVoidFollowTransaction(t)
                && Boolean.TRUE.equals(base.get("EMAIL_VOID"))
                && agencyAllows(t, PayListActionService.PayFollowAction.EMAIL_VOID)
                && withinEmailVoidWindow(t, env, ref));
        out.put("AUTO_REFUND", Boolean.TRUE.equals(base.get("AUTO_REFUND"))
                && agencyAllows(t, PayListActionService.PayFollowAction.AUTO_REFUND)
                && withinAutoRefundDays(t, env, REFUND_DAY_ZONE));
        out.put("FORCE_REFUND", Boolean.TRUE.equals(base.get("FORCE_REFUND"))
                && agencyAllows(t, PayListActionService.PayFollowAction.FORCE_REFUND)
                && withinForceRefundDays(t, env, REFUND_DAY_ZONE));
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
     * URL 결제(ElementPay) 당일환불: 전역 Y + 단계별 허용(ADMIN은 전역만) + 태국 결제일 당일.
     * 환불처리는 기존 자동환불과 동일 API.
     */
    private boolean sameDayRefundOpen(AppUser viewer, PgTrnsctn t, HqNotifyEnvConfig env) {
        if (t == null || env == null || !PgVendor.isElementPayFamily(t.getVan())) {
            return false;
        }
        if (!yn(env.getEpSameDayRefundYn())) {
            return false;
        }
        if (!agencyAllowsSameDayRefund(t)) {
            return false;
        }
        if (!withinElementPaySameDayRefund(t)) {
            return false;
        }
        if (viewer != null && "ADMIN".equalsIgnoreCase(viewer.getRole())) {
            return true;
        }
        if (viewer == null) {
            return false;
        }
        Map<String, Object> org = authService.getOrgInfo(viewer.getUsername());
        if (org == null) {
            return false;
        }
        String level = String.valueOf(org.getOrDefault("orgLevel", "")).trim().toUpperCase(Locale.ROOT);
        OrgLevelPayFollowCap cap = capRepository.findById(level).orElse(null);
        return cap != null && yn(cap.getSameDayRefundYn());
    }

    /** 태국(Asia/Bangkok) 달력 기준 결제일 = 오늘. */
    private static boolean withinElementPaySameDayRefund(PgTrnsctn t) {
        ZonedDateTime approval = approvalAtZone(t, REFUND_DAY_ZONE);
        if (approval == null) {
            return false;
        }
        LocalDate today = ZonedDateTime.now(REFUND_DAY_ZONE).toLocalDate();
        return today.equals(approval.toLocalDate());
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
            case MANUAL_VOID -> yn(cap.getManualVoidYn());
            case AUTO_REFUND -> yn(cap.getAutoRefundYn());
            case MANUAL_REFUND -> yn(cap.getManualRefundYn());
            case FORCE_REFUND -> yn(cap.getForceRefundYn());
        };
    }

    boolean agencyAllows(PgTrnsctn t, PayListActionService.PayFollowAction action) {
        return PgAgencyPayFollowCapability.allows(resolveAgencyForVan(t != null ? t.getVan() : null), action);
    }

    boolean agencyAllowsSameDayRefund(PgTrnsctn t) {
        return PgAgencyPayFollowCapability.allowsSameDayRefund(resolveAgencyForVan(t != null ? t.getVan() : null));
    }

    private PgAgency resolveAgencyForVan(String van) {
        if (van == null || van.isBlank() || pgAgencyRepository == null) {
            return fallbackAgency(van);
        }
        String key = van.trim();
        Optional<PgAgency> exact = pgAgencyRepository.findByPgCd(key);
        if (exact.isPresent()) {
            return exact.get();
        }
        String norm = PgVendor.normalizePgCdKey(key);
        if (!norm.equalsIgnoreCase(key)) {
            Optional<PgAgency> alt = pgAgencyRepository.findByPgCd(norm);
            if (alt.isPresent()) {
                return alt.get();
            }
        }
        return fallbackAgency(van);
    }

    private static PgAgency fallbackAgency(String van) {
        PgAgency fake = new PgAgency();
        fake.setPgCd(van);
        PgAgencyPayFollowCapability.applyFamilyDefaults(fake);
        return fake;
    }

    public void assertMayExecute(AppUser user, String trnId, PayListActionService.PayFollowAction action) {
        HqNotifyEnvConfig env = hqNotifyEnvService.getOrCreate();
        switch (action) {
            case AUTO_VOID -> requireYn(env.getAutoVoidYn(), "무효처리");
            case EMAIL_VOID -> requireYn(env.getEmailVoidYn(), "이메일 무효");
            case AUTO_REFUND -> requireYn(env.getAutoRefundYn(), "환불처리");
            case FORCE_REFUND -> requireYn(env.getForceRefundYn(), "강제환불");
            case MANUAL_VOID -> requireYn(env.getManualVoidYn(), "수동무효");
            case MANUAL_REFUND -> requireYn(env.getManualRefundYn(), "수동환불");
        }
        if (user == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        PgTrnsctn t = trnsctnRepository.findById(trnId.trim())
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다."));
        if (!"10".equals(t.getStatus())) {
            throw new IllegalStateException("승인(결제) 완료 건만 후속조치할 수 있습니다.");
        }
        boolean jpay = isJpayManualFollowTransaction(t);
        if (action == PayListActionService.PayFollowAction.EMAIL_VOID && !isEmailVoidFollowTransaction(t)) {
            throw new IllegalStateException("이메일무효는 ChillPay 거래만 사용할 수 있습니다.");
        }
        if (jpay && action == PayListActionService.PayFollowAction.AUTO_VOID) {
            throw new IllegalStateException(
                    "JPAY 거래는 무효처리·이메일 무효를 사용할 수 없습니다. 수동무효 또는 JPAY 포털에서 처리하세요.");
        }
        if (isRefundApiFollowTransaction(t) && action == PayListActionService.PayFollowAction.AUTO_VOID) {
            throw new IllegalStateException(
                    "이 거래는 승인 후 무효(void) API가 없습니다. 결제내역의 환불처리·강제환불을 사용하세요.");
        }
        if (!jpay && (action == PayListActionService.PayFollowAction.MANUAL_VOID
                || action == PayListActionService.PayFollowAction.MANUAL_REFUND)) {
            throw new IllegalStateException("수동무효·수동환불은 JPAY 거래만 지원합니다.");
        }
        if (!agencyAllows(t, action)) {
            throw new IllegalStateException("이 결제대행사는 해당 후속조치를 ICOPAY에서 허용하지 않습니다.");
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
                    case MANUAL_VOID -> merchantActionEffective(mp.getPayFollowManualVoidYn());
                    case AUTO_REFUND -> merchantActionEffective(mp.getPayFollowAutoRefundYn());
                    case MANUAL_REFUND -> merchantActionEffective(mp.getPayFollowManualRefundYn());
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
        if (action == PayListActionService.PayFollowAction.MANUAL_VOID
                || action == PayListActionService.PayFollowAction.MANUAL_REFUND) {
            return;
        }
        ZoneId ref = resolvePayFollowZone(env);
        switch (action) {
            case AUTO_VOID -> {
                if (!withinAutoVoidWindow(t, env, ref)) {
                    throw new IllegalStateException(
                            "무효처리는 승인일(시간 선택 국가 기준) 당일, 설정한 시작~마감 시각 안에서만 가능합니다.");
                }
            }
            case EMAIL_VOID -> {
                if (!withinEmailVoidWindow(t, env, ref)) {
                    throw new IllegalStateException(
                            "이메일 무효는 승인일(기준 Zone) 당일, 설정한 시작~마감 시각 안에서만 가능합니다.");
                }
            }
            case AUTO_REFUND -> {
                if (!withinAutoRefundDays(t, env, REFUND_DAY_ZONE) && !sameDayRefundOpen(user, t, env)) {
                    throw new IllegalStateException(
                            "환불처리 가능 기간이 아닙니다. (태국 기준 결제일 익일부터이거나, URL 결제 당일환불이 켜진 당일인지 확인)");
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
        if (!yn(cap.getManualVoidYn()) && mp.getPayFollowManualVoidYn() != null && yn(mp.getPayFollowManualVoidYn())) {
            mp.setPayFollowManualVoidYn("N");
        }
        if (!yn(cap.getManualRefundYn()) && mp.getPayFollowManualRefundYn() != null && yn(mp.getPayFollowManualRefundYn())) {
            mp.setPayFollowManualRefundYn("N");
        }
        if (!yn(cap.getForceRefundYn()) && mp.getPayFollowForceRefundYn() != null && yn(mp.getPayFollowForceRefundYn())) {
            mp.setPayFollowForceRefundYn("N");
        }
        if (!yn(cap.getAutoVoidYn()) && !yn(cap.getEmailVoidYn()) && !yn(cap.getManualVoidYn())
                && !yn(cap.getAutoRefundYn()) && !yn(cap.getManualRefundYn()) && !yn(cap.getForceRefundYn())) {
            mp.setPayFollowMerchantUseYn("N");
        }
    }
}
