package com.pg.service;

import com.pg.config.ChillPayProperties;
import com.pg.dto.ChillPayDirectCreditRequest;
import com.pg.dto.ChillPayDirectCreditResponse;
import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantPgBinding;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantPgBindingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;

/**
 * ChillPay DirectCredit API 연동 서비스.
 * 설정은 본사설정 > API 구성 세팅에서 저장한 DB 값을 사용합니다.
 */
@Service
public class ChillPayService {

    private static final Logger log = LoggerFactory.getLogger(ChillPayService.class);
    private static final String CCD_SCRIPT_SANDBOX = "https://sandbox-bankdemo3.chillpay.co/js/ccdpayment.js";
    private static final String CCD_SCRIPT_PROD = "https://cdn.chill.credit/js/ccdpayment.js";
    private static final String DIRECT_CREDIT_SANDBOX = "https://sandbox-api-directcredit.chillpay.co";
    private static final String DIRECT_CREDIT_PROD = "https://api-directcredit.chillpay.co";

    private static final String PG_CD_CHILLPAY = "CHILLPAY";

    private final ChillPayProperties props;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public ChillPayService(ChillPayProperties props, HqApiConfigRepository hqApiConfigRepository,
                          MerchantPgBindingRepository merchantPgBindingRepository) {
        this.props = props;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
    }

    /** 가맹점 orgUnitId가 있으면 해당 가맹점의 ChillPay 설정(결제대행사 설정) 우선, 없으면 본사 설정 */
    private Config resolveConfig(Long merchantOrgUnitId) {
        if (merchantOrgUnitId != null) {
            Optional<MerchantPgBinding> binding = merchantPgBindingRepository.findFirstByOrgUnitIdAndPgCdAndOperationalYn(merchantOrgUnitId, PG_CD_CHILLPAY, "Y");
            if (binding.isPresent()) {
                MerchantPgBinding b = binding.get();
                String mc = (b.getMid() != null && !b.getMid().isEmpty()) ? b.getMid() : null;
                String ak = (b.getApiKey() != null && !b.getApiKey().isEmpty()) ? b.getApiKey() : null;
                String mk = (b.getIvKey() != null && !b.getIvKey().isEmpty()) ? b.getIvKey() : null;
                if (ak != null && mk != null) {
                    Config base = resolveConfigFromHq();
                    return new Config(mc != null ? mc : base.merchantCode(), ak, mk, base.routeNo(), base.sandbox());
                }
            }
        }
        return resolveConfigFromHq();
    }

    /** 본사설정(API 구성 세팅) 또는 application.yml에서 ChillPay 설정 조회 */
    private Config resolveConfigFromHq() {
        Optional<HqApiConfig> opt = hqApiConfigRepository.findAll().stream().findFirst();
        if (opt.isPresent()) {
            HqApiConfig c = opt.get();
            String apiKey = (c.getChillpayApiKey() != null && !c.getChillpayApiKey().isEmpty()) ? c.getChillpayApiKey() : props.getApiKey();
            String md5Key = (c.getChillpayMd5Key() != null && !c.getChillpayMd5Key().isEmpty()) ? c.getChillpayMd5Key() : props.getMd5Key();
            String merchantCode = (c.getChillpayMerchantCode() != null && !c.getChillpayMerchantCode().isEmpty()) ? c.getChillpayMerchantCode() : props.getMerchantCode();
            int routeNo = (c.getChillpayRouteNo() != null) ? c.getChillpayRouteNo() : props.getRouteNo();
            boolean sandbox = !"N".equalsIgnoreCase(c.getChillpaySandbox());
            return new Config(merchantCode, apiKey, md5Key, routeNo, sandbox);
        }
        return new Config(props.getMerchantCode(), props.getApiKey(), props.getMd5Key(), props.getRouteNo(), props.isSandbox());
    }

    private record Config(String merchantCode, String apiKey, String md5Key, int routeNo, boolean sandbox) {
        String getCcdScriptUrl() { return sandbox ? CCD_SCRIPT_SANDBOX : CCD_SCRIPT_PROD; }
        String getPaymentApiUrl() { return (sandbox ? DIRECT_CREDIT_SANDBOX : DIRECT_CREDIT_PROD) + "/api/v1/payment"; }
    }

    /**
     * ChillPay DirectCredit 결제 API 호출.
     */
    /** merchantOrgUnitId: 가맹점 등록 시 결제대행사 설정에 ChillPay를 운영대상으로 등록한 경우 해당 가맹점 설정 사용 */
    public ChillPayDirectCreditResponse requestPayment(
            String orderNo, String customerId, Long amount, String directCreditToken,
            String phoneNumber, String description, String ipAddress, String custEmail,
            Long merchantOrgUnitId) {

        Config cfg = resolveConfig(merchantOrgUnitId);
        if (cfg.apiKey() == null || cfg.apiKey().isEmpty()) {
            throw new IllegalStateException("ChillPay API Key가 설정되지 않았습니다. 본사설정 > API 구성 세팅 또는 가맹점 등록 > 결제대행사 설정에서 ChillPay 정보를 입력하세요.");
        }
        if (cfg.md5Key() == null || cfg.md5Key().isEmpty()) {
            throw new IllegalStateException("ChillPay MD5 Key가 설정되지 않았습니다. 본사설정 > API 구성 세팅 또는 가맹점 등록 > 결제대행사 설정에서 ChillPay 정보를 입력하세요.");
        }

        ChillPayDirectCreditRequest req = new ChillPayDirectCreditRequest();
        req.setOrderNo(orderNo != null ? orderNo : "ORD" + System.currentTimeMillis());
        req.setCustomerId(customerId != null ? customerId : "guest");
        req.setAmount(amount != null ? amount : 0L);
        req.setDirectCreditToken(directCreditToken);
        req.setPhoneNumber(phoneNumber != null ? phoneNumber : "");
        req.setDescription(description != null ? description : "");
        req.setRouteNo(cfg.routeNo());
        req.setIPAddress(ipAddress != null ? ipAddress : "127.0.0.1");
        req.setCustEmail(custEmail != null ? custEmail : "");

        String concat = req.toConcatString();
        String checkSum = md5(concat + cfg.md5Key());
        req.setCheckSum(checkSum);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Api-Key", cfg.apiKey());
        headers.set("Merchant-Code", cfg.merchantCode());

        HttpEntity<ChillPayDirectCreditRequest> entity = new HttpEntity<>(req, headers);
        String url = cfg.getPaymentApiUrl();

        log.info("ChillPay 요청: {} orderNo={} amount={}", url, req.getOrderNo(), req.getAmount());

        try {
            ResponseEntity<ChillPayDirectCreditResponse> res = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    ChillPayDirectCreditResponse.class
            );
            ChillPayDirectCreditResponse body = res.getBody();
            if (body != null && body.getData() != null) {
                log.info("ChillPay 응답: status={} paymentStatus={}", body.getStatus(), body.getData().getPaymentStatus());
            }
            return body;
        } catch (Exception e) {
            log.error("ChillPay API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("ChillPay 결제 요청 실패: " + e.getMessage(), e);
        }
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }

    /** 결제 페이지용 설정. merchantOrgUnitId 있으면 해당 가맹점 ChillPay 설정 사용 */
    public Map<String, Object> getConfigForFrontend(Long merchantOrgUnitId) {
        Config cfg = resolveConfig(merchantOrgUnitId);
        return Map.of(
                "ccdScriptUrl", cfg.getCcdScriptUrl(),
                "merchantCode", cfg.merchantCode(),
                "routeNo", cfg.routeNo(),
                "sandbox", cfg.sandbox()
        );
    }
}
