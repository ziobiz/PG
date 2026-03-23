package com.pg.service;

import com.pg.entity.HqNotifyTarget;
import com.pg.repository.HqNotifyTargetRepository;
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

    public HqNotifyTargetService(HqNotifyTargetRepository repository, HqNotifyEnvService hqNotifyEnvService) {
        this.repository = repository;
        this.hqNotifyEnvService = hqNotifyEnvService;
    }

    public List<Map<String, Object>> list() {
        return repository.findAllByOrderByIdDesc().stream().map(this::toMap).toList();
    }

    /**
     * 동일 대상명으로 CALLBACK·RESULT 노티 URL을 각각 한 건씩 생성합니다.
     * URL 경로 코드는 cb/rs + 6자(영숫자)로 짧게 부여합니다.
     */
    @Transactional
    public Map<String, Object> createPair(String targetName, HttpServletRequest req) {
        String name = targetName == null ? "" : targetName.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("노티 대상 이름을 입력하세요.");
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
        repository.save(tCb);

        HqNotifyTarget tRs = new HqNotifyTarget();
        tRs.setTargetCode(rsCode);
        tRs.setTargetName(name);
        tRs.setTargetUrl(ingressBase + "/" + rsCode);
        tRs.setChannelType(CH_RESULT);
        tRs.setUseYn("Y");
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
        return m;
    }

    private static String channelLabelKo(String ch) {
        if (CH_RESULT.equals(ch)) return "RESULT (브라우저·리다이렉트)";
        return "CALLBACK (서버 노티)";
    }
}
