package com.pg.service;

import com.pg.entity.MerchantNotifyUrl;
import com.pg.entity.NotiProvisionLog;
import com.pg.repository.MerchantNotifyUrlRepository;
import com.pg.repository.NotiProvisionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 가맹 {@code JPAY_NOTIFY}/{@code JPAY_CALLBACK}(노티미들웨어 URL) 저장·보강.
 * 노티생성(Provision) 직후·업체 상세 조회 시 이력과 동기화한다.
 */
@Service
public class MerchantJpayNotifyUrlSyncService {

    private static final Logger log = LoggerFactory.getLogger(MerchantJpayNotifyUrlSyncService.class);

    private final MerchantNotifyUrlRepository merchantNotifyUrlRepository;
    private final NotiProvisionLogRepository notiProvisionLogRepository;

    public MerchantJpayNotifyUrlSyncService(MerchantNotifyUrlRepository merchantNotifyUrlRepository,
                                           NotiProvisionLogRepository notiProvisionLogRepository) {
        this.merchantNotifyUrlRepository = merchantNotifyUrlRepository;
        this.notiProvisionLogRepository = notiProvisionLogRepository;
    }

    /**
     * 가맹 JPAY 수신통보 URL을 덮어쓴다(비어 있지 않은 타입만).
     * delete + insert + flush 로 유니크 제약·영속성 이슈를 피한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(long orgUnitId, String notifyUrl, String callbackUrl) {
        String notify = trimUrl(notifyUrl);
        String callback = trimUrl(callbackUrl);
        if (notify.isEmpty() && callback.isEmpty()) {
            log.warn("JPAY 수신통보 URL 저장 생략 — URL 비어 있음 orgUnitId={}", orgUnitId);
            return;
        }
        if (!notify.isEmpty()) {
            replaceOne(orgUnitId, MerchantNotifyUrl.URL_TYPE_JPAY_NOTIFY, notify);
        }
        if (!callback.isEmpty()) {
            replaceOne(orgUnitId, MerchantNotifyUrl.URL_TYPE_JPAY_CALLBACK, callback);
        }
        merchantNotifyUrlRepository.flush();
        String savedN = find(orgUnitId, MerchantNotifyUrl.URL_TYPE_JPAY_NOTIFY);
        String savedC = find(orgUnitId, MerchantNotifyUrl.URL_TYPE_JPAY_CALLBACK);
        if ((!notify.isEmpty() && savedN.isEmpty()) || (!callback.isEmpty() && savedC.isEmpty())) {
            throw new IllegalStateException(
                    "가맹 JPAY 수신통보 URL 저장에 실패했습니다. orgUnitId=" + orgUnitId
                            + " notify=" + (!notify.isEmpty()) + " callback=" + (!callback.isEmpty()));
        }
        log.info("가맹 JPAY 수신통보 URL 저장 orgUnitId={} notifyLen={} callbackLen={}",
                orgUnitId, savedN.length(), savedC.length());
    }

    /**
     * 가맹 JPAY URL이 비어 있고, 최신 노티생성 이력에 URL이 있으면 이력 값으로 보강한다.
     *
     * @return true if URLs were written
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean backfillFromLatestProvisionLogIfEmpty(long orgUnitId) {
        String curN = find(orgUnitId, MerchantNotifyUrl.URL_TYPE_JPAY_NOTIFY);
        String curC = find(orgUnitId, MerchantNotifyUrl.URL_TYPE_JPAY_CALLBACK);
        if (!curN.isEmpty() || !curC.isEmpty()) {
            return false;
        }
        Optional<NotiProvisionLog> logOpt =
                notiProvisionLogRepository.findFirstByOrgUnitIdOrderByProvisionedAtDescIdDesc(orgUnitId);
        if (logOpt.isEmpty()) {
            return false;
        }
        NotiProvisionLog pl = logOpt.get();
        String n = trimUrl(pl.getJpayNotifyUrl());
        String c = trimUrl(pl.getJpayCallbackUrl());
        if (n.isEmpty() && c.isEmpty() && pl.getSlotNo() != null && pl.getSlotNo() > 0) {
            int slot = pl.getSlotNo();
            n = "https://noti.icopay.net/noti/callback/j" + slot;
            c = "https://noti.icopay.net/noti/result/j" + slot;
        }
        if (n.isEmpty() && c.isEmpty()) {
            return false;
        }
        if (!n.isEmpty()) {
            replaceOne(orgUnitId, MerchantNotifyUrl.URL_TYPE_JPAY_NOTIFY, n);
        }
        if (!c.isEmpty()) {
            replaceOne(orgUnitId, MerchantNotifyUrl.URL_TYPE_JPAY_CALLBACK, c);
        }
        merchantNotifyUrlRepository.flush();
        log.info("가맹 JPAY 수신통보 URL을 노티생성 이력에서 보강 orgUnitId={} logId={}", orgUnitId, pl.getId());
        return true;
    }

    public String find(long orgUnitId, String urlType) {
        return merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(orgUnitId, urlType)
                .map(MerchantNotifyUrl::getNotiUrl)
                .filter(u -> u != null && !u.isBlank())
                .map(String::trim)
                .orElse("");
    }

    private void replaceOne(long orgUnitId, String urlType, String url) {
        merchantNotifyUrlRepository.deleteByOrgUnitIdAndUrlTypeIn(orgUnitId, List.of(urlType));
        merchantNotifyUrlRepository.flush();
        MerchantNotifyUrl row = new MerchantNotifyUrl();
        row.setOrgUnitId(orgUnitId);
        row.setUrlType(urlType);
        row.setNotiUrl(url);
        row.setUseYn("Y");
        merchantNotifyUrlRepository.save(row);
    }

    private static String trimUrl(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return "";
        }
        String lower = t.toLowerCase();
        if ("https://".equals(lower) || "http://".equals(lower)) {
            return "";
        }
        return t;
    }
}
