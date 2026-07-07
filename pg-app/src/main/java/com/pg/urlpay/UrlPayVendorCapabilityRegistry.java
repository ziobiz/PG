package com.pg.urlpay;

import com.pg.entity.PgAgency;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.PgAgencyRepository;
import com.pg.service.ChillPayService;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * PG {@code pg_cd} → URL 결제 플랫폼 기능 매핑.
 * <p><b>신규 PG 추가:</b> 이 클래스에 분기(또는 전용 {@link UrlPayVendorCapability} 팩토리 메서드)를 추가하고,
 * 필요 시 전용 결제 페이지·{@link UrlPaySaleChannel}·{@link UrlPayCheckoutContextEnricher} 를 등록합니다.
 * 본사 URL결제설정·다통화·다국어는 수정 없이 자동 적용됩니다.
 */
@Component
public class UrlPayVendorCapabilityRegistry {

    private final PgAgencyRepository pgAgencyRepository;

    public UrlPayVendorCapabilityRegistry(PgAgencyRepository pgAgencyRepository) {
        this.pgAgencyRepository = pgAgencyRepository;
    }

    public UrlPayVendorCapability resolve(String operationalPgCd) {
        String pg = operationalPgCd != null ? operationalPgCd.trim() : "";
        Optional<PgAgency> agency = pg.isEmpty() ? Optional.empty() : pgAgencyRepository.findByPgCd(pg);
        boolean urlPay = agency.map(this::isUrlPayAgency).orElse(false);
        boolean urlRepayAgency = agency.map(this::isUrlRepayAgency).orElse(false);

        if (PgVendor.isJpayFamily(pg)) {
            return jpayCapability(pg, urlPay, urlRepayAgency);
        }
        if (PgVendor.isEximbayFamily(pg)) {
            return eximbayCapability(pg, urlPay, urlRepayAgency);
        }
        if (PgVendor.isElementPayFamily(pg)) {
            return elementPayCapability(pg, urlPay, urlRepayAgency);
        }
        if (PgVendor.isChillPayFamily(pg) || ChillPayService.isChillPayFamilyPgCd(pg)) {
            return chillPayCapability(pg, urlPay, urlRepayAgency);
        }
        return genericCapability(pg, urlPay, urlRepayAgency);
    }

    /**
     * @deprecated {@link #resolve(String)} 사용. ChillPayService 정적 호출 호환.
     */
    @Deprecated
    public static String resolveInlineWidgetKindLegacy(String pgCd) {
        String pg = pgCd != null ? pgCd.trim() : "";
        if (PgVendor.isJpayFamily(pg)) {
            return UrlPayInlineWidgetKind.JPAY_INLINE;
        }
        if (PgVendor.isEximbayFamily(pg)) {
            return UrlPayInlineWidgetKind.EXIMBAY_SDK;
        }
        if (PgVendor.isElementPayFamily(pg)) {
            return UrlPayInlineWidgetKind.ELEMENTPAY_INLINE;
        }
        if (PgVendor.isChillPayFamily(pg) || ChillPayService.isChillPayFamilyPgCd(pg)) {
            return UrlPayInlineWidgetKind.CHILLPAY_CCD;
        }
        return UrlPayInlineWidgetKind.UNSUPPORTED_INLINE;
    }

    private UrlPayVendorCapability jpayCapability(String pg, boolean urlPay, boolean urlRepayAgency) {
        boolean repayApi = false;
        return new UrlPayVendorCapability(
                PgVendor.JPAY,
                pg,
                UrlPayInlineWidgetKind.JPAY_INLINE,
                "/checkout/",
                NeutralCheckoutRoute.EMBED_SCRIPT_PATH,
                UrlPaySaleChannel.JPAY_INLINE_SALE,
                urlPay,
                urlRepayAgency,
                urlRepayAgency && repayApi);
    }

    /**
     * Eximbay — ready→fgkey→JS SDK 결제창. 정기결제는 tokenbilling(REBILL) 서버측 재청구로 별도 처리하며,
     * 공개 URL 재결제 플로우는 제공하지 않으므로 재결제 URL 은 비활성(repayApi=false)으로 둔다.
     */
    private UrlPayVendorCapability eximbayCapability(String pg, boolean urlPay, boolean urlRepayAgency) {
        boolean repayApi = false;
        return new UrlPayVendorCapability(
                PgVendor.EXIMBAY,
                pg,
                UrlPayInlineWidgetKind.EXIMBAY_SDK,
                "/checkout/",
                NeutralCheckoutRoute.EMBED_SCRIPT_PATH,
                UrlPaySaleChannel.EXIMBAY_READY_SALE,
                urlPay,
                urlRepayAgency,
                urlRepayAgency && repayApi);
    }

    /** ElementPay — THB 전용 initPayment(카드·PromptPay). 재결제 URL 미지원. */
    private UrlPayVendorCapability elementPayCapability(String pg, boolean urlPay, boolean urlRepayAgency) {
        return new UrlPayVendorCapability(
                PgVendor.ELEMENTPAY,
                pg,
                UrlPayInlineWidgetKind.ELEMENTPAY_INLINE,
                "/checkout/",
                NeutralCheckoutRoute.EMBED_SCRIPT_PATH,
                UrlPaySaleChannel.ELEMENTPAY_INIT_PAYMENT,
                urlPay,
                urlRepayAgency,
                false);
    }

    private UrlPayVendorCapability chillPayCapability(String pg, boolean urlPay, boolean urlRepayAgency) {
        boolean repayApi = true;
        return new UrlPayVendorCapability(
                PgVendor.CHILLPAY,
                pg,
                UrlPayInlineWidgetKind.CHILLPAY_CCD,
                "/checkout/",
                NeutralCheckoutRoute.EMBED_SCRIPT_PATH,
                UrlPaySaleChannel.CHILLPAY_DIRECT_CREDIT,
                urlPay,
                urlRepayAgency,
                urlRepayAgency && repayApi);
    }

    /** 향후 PG — 인라인·재결제 미등록 시 관리자에 재결제 URL 비활성 안내 */
    private UrlPayVendorCapability genericCapability(String pg, boolean urlPay, boolean urlRepayAgency) {
        String family = pg.isEmpty() ? "UNKNOWN" : PgVendor.normalizePgCdKey(pg).split("_")[0];
        return new UrlPayVendorCapability(
                family,
                pg,
                UrlPayInlineWidgetKind.UNSUPPORTED_INLINE,
                "/checkout/",
                NeutralCheckoutRoute.EMBED_SCRIPT_PATH,
                UrlPaySaleChannel.NOT_REGISTERED,
                urlPay,
                urlRepayAgency,
                false);
    }

    private boolean isUrlPayAgency(PgAgency a) {
        return a != null
                && "Y".equalsIgnoreCase(nullToEmpty(a.getUseYn()))
                && "Y".equalsIgnoreCase(nullToEmpty(a.getIntegUrlPayYn()));
    }

    private boolean isUrlRepayAgency(PgAgency a) {
        return a != null
                && "Y".equalsIgnoreCase(nullToEmpty(a.getUseYn()))
                && "Y".equalsIgnoreCase(nullToEmpty(a.getIntegUrlPayRepayYn()));
    }

    private static String nullToEmpty(String s) {
        return s != null ? s.trim() : "";
    }
}
