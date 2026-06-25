package com.pg.service;

import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 가맹 챗봇 상단 로고 업로드: 2MB(설정 가능) 이하가 되도록 자동 재압축.
 * 해상도 힌트는 본사 AI설정 {@code config_json} 키(옵션)와, {@code chatbot_logo_llm_tune_yn=Y} 일 때
 * {@link ChatbotLlmCompletionService} 로 LLM이 제안합니다. 실제 리사이즈는 ImageIOJPEG 입니다.
 */
@Service
public class ChatbotHeaderLogoUploadService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotHeaderLogoUploadService.class);

    private static final long MAX_ORIGINAL_UPLOAD_BYTES = 40L * 1024 * 1024;
    private static final Pattern INT_PATTERN = Pattern.compile("-?\\d+");

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final HqChatbotAiSettingsService hqChatbotAiSettingsService;
    private final ChatbotLlmCompletionService chatbotLlmCompletionService;
    private final OrgUnitChangeAuditService orgUnitChangeAuditService;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public ChatbotHeaderLogoUploadService(OrgUnitRepository orgUnitRepository,
                                         MerchantProfileRepository merchantProfileRepository,
                                         HqChatbotAiSettingsService hqChatbotAiSettingsService,
                                         ChatbotLlmCompletionService chatbotLlmCompletionService,
                                         OrgUnitChangeAuditService orgUnitChangeAuditService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.hqChatbotAiSettingsService = hqChatbotAiSettingsService;
        this.chatbotLlmCompletionService = chatbotLlmCompletionService;
        this.orgUnitChangeAuditService = orgUnitChangeAuditService;
    }

    public record UploadResult(String publicUrlRelative, boolean usedLlmTuningHint, String detailMessage) {}

    @Transactional
    public UploadResult processAndPersist(Long merchantOrgUnitId, String compId, MultipartFile file) throws IOException {
        return processAndPersistInternal(merchantOrgUnitId, compId, file, "chatbot-header", "hlogo_",
                MerchantProfile::getChatbotHeaderLogoUrl, MerchantProfile::setChatbotHeaderLogoUrl,
                "[업체정보] 챗봇 상단 로고 URL");
    }

    @Transactional
    public UploadResult processAndPersistWebPaymentHeader(Long merchantOrgUnitId, String compId, MultipartFile file)
            throws IOException {
        return processAndPersistInternal(merchantOrgUnitId, compId, file, "web-payment-header", "wlogo_",
                MerchantProfile::getWebPaymentHeaderLogoUrl, MerchantProfile::setWebPaymentHeaderLogoUrl,
                "[업체정보] 웹결제 상단 로고 URL");
    }

    @Transactional
    public UploadResult processAndPersistSplitPayHeader(Long merchantOrgUnitId, String compId, MultipartFile file)
            throws IOException {
        return processAndPersistInternal(merchantOrgUnitId, compId, file, "split-pay-header", "slogo_",
                MerchantProfile::getSplitPayHeaderLogoUrl, MerchantProfile::setSplitPayHeaderLogoUrl,
                "[업체정보] 분할결제 상단 로고 URL");
    }

    private UploadResult processAndPersistInternal(Long merchantOrgUnitId, String compId, MultipartFile file,
                                                   String storageSubdir, String filePrefix,
                                                   java.util.function.Function<MerchantProfile, String> urlGetter,
                                                   java.util.function.BiConsumer<MerchantProfile, String> urlSetter,
                                                   String auditLabel) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일을 선택하세요.");
        }
        if (file.getSize() > MAX_ORIGINAL_UPLOAD_BYTES) {
            throw new IllegalArgumentException("업로드 이미지는 40MB 이하여야 합니다.");
        }
        String extIn = resolveExtension(file.getOriginalFilename(), file.getContentType());
        if (extIn == null || (!extIn.equalsIgnoreCase("png") && !extIn.equalsIgnoreCase("jpg") && !extIn.equalsIgnoreCase("jpeg"))) {
            throw new IllegalArgumentException("PNG 또는 JPG만 업로드할 수 있습니다.");
        }

        Map<String, Object> cfg = hqChatbotAiSettingsService.rawConfigForServerUse();
        long targetMax = parsePositiveLong(cfg.get("chatbot_logo_target_max_bytes"), 2097152L); // 2 MiB default
        int defaultMaxEdge = clamp(parsePositiveInt(cfg.get("chatbot_logo_max_edge_px"), 1024), 320, 2048);
        float startQuality = clamp01(parseQuality(cfg.get("chatbot_logo_jpeg_quality_start")));

        boolean llmTune = "Y".equalsIgnoreCase(str(cfg.get("chatbot_logo_llm_tune_yn")));
        int maxEdge = defaultMaxEdge;
        boolean usedLlm = false;
        if (llmTune) {
            int suggested = llmSuggestMaxEdge(cfg, targetMax / 1024);
            if (suggested > 0) {
                maxEdge = suggested;
                usedLlm = true;
            }
        }
        maxEdge = clamp(maxEdge, 320, 2048);

        byte[] optimized = optimizeLogoBytes(file.getBytes(), targetMax, maxEdge, startQuality);
        String fileName = filePrefix + UUID.randomUUID().toString().substring(0, 8) + ".jpg";
        Path basePath = Paths.get(System.getProperty("user.dir"), uploadDir, storageSubdir,
                sanitizeCompId(compId)).normalize();
        Files.createDirectories(basePath);
        Path targetPath = basePath.resolve(fileName);
        Files.write(targetPath, optimized);

        String relative = "/uploads/" + storageSubdir + "/" + sanitizeCompId(compId) + "/" + fileName;

        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId)
                .orElseThrow(() -> new IllegalArgumentException("가맹 프로필을 찾을 수 없습니다."));
        String before = nz(urlGetter.apply(mp));
        String stored = relative.length() > 500 ? relative.substring(0, 500) : relative;
        urlSetter.accept(mp, stored);
        merchantProfileRepository.save(mp);

        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(merchantOrgUnitId);
        if (ouOpt.isPresent()) {
            OrgUnit ou = ouOpt.get();
            orgUnitChangeAuditService.appendIfChanged(ou.getId(), nz(ou.getCode()), nz(ou.getName()),
                    auditLabel, basenameForAudit(before), basenameForAudit(stored));
        }

        String msg = optimized.length <= targetMax
                ? ("최종 " + optimized.length / 1024 + "KB (설정 목표 이하)") + (usedLlm ? " · LLM 해상도 힌트 사용" : "")
                : ("최종 " + optimized.length / 1024 + "KB · 목표(" + targetMax / 1024 + "KB)보다 클 수 있어 추가 수동 설정을 검토하세요.");
        return new UploadResult(relative, usedLlm, msg);
    }

    /** MERCHANT 검증 포함 */
    @Transactional
    public Optional<OrgUnit> requireMerchantOrg(String compId) {
        return orgUnitRepository.findByCode(sanitizeCompId(compId))
                .filter(ou -> ou.getOrgLevel() == OrgLevel.MERCHANT);
    }

    private int llmSuggestMaxEdge(Map<String, Object> rawCfg, long targetMaxKb) {
        try {
            String sys = "You output exactly one decimal integer only (no words), between 420 and 1200 inclusive: "
                    + "recommended maximum width-or-height in pixels for a JPEG logo shown in a small circular bubble on mobile.";
            String user = "Target final file soft limit about " + targetMaxKb + " KB after server-side JPEG optimization.";
            List<Map<String, String>> msgs = List.of(Map.of("role", "user", "content", user));
            String reply = chatbotLlmCompletionService.completeChat(rawCfg, sys, msgs,
                    com.pg.util.ChatbotLlmUsage.GENERAL);
            return parseFirstIntClamp(reply, 420, 1200);
        } catch (Exception e) {
            log.warn("chatbot logo LLM tuning skipped: {}", e.getMessage());
            return -1;
        }
    }

    private static int parseFirstIntClamp(String reply, int min, int max) {
        if (reply == null) {
            return -1;
        }
        Matcher m = INT_PATTERN.matcher(reply.trim());
        if (!m.find()) {
            return -1;
        }
        try {
            return clamp(Integer.parseInt(m.group()), min, max);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private byte[] optimizeLogoBytes(byte[] original, long targetMaxBytes, int maxEdge, float startQ) throws IOException {
        BufferedImage read = ImageIO.read(new java.io.ByteArrayInputStream(original));
        if (read == null) {
            throw new IllegalArgumentException("이미지를 읽을 수 없습니다.");
        }
        BufferedImage rgb = flattenToRgb(read);
        int w = rgb.getWidth();
        int h = rgb.getHeight();
        double edgeScale = w > 0 && h > 0 ? Math.min(1.0, (double) maxEdge / Math.max(w, h)) : 1.0;
        BufferedImage working = edgeScale >= 1.0 - 1e-9 ? rgb : scale(rgb, edgeScale);

        float q = startQ;
        double extraScale = 1.0;
        byte[] best = writeJpeg(working, q);
        int guard = 0;
        while (best.length > targetMaxBytes && guard++ < 18) {
            if (q > 0.52f) {
                q = Math.max(0.52f, q - 0.065f);
            } else {
                extraScale *= 0.87;
                if (extraScale < 0.28) {
                    break;
                }
                working = scale(rgb, edgeScale * extraScale);
                q = Math.min(0.9f, startQ);
            }
            best = writeJpeg(working, q);
        }
        return best;
    }

    private static BufferedImage flattenToRgb(BufferedImage src) {
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, src.getWidth(), src.getHeight());
            g.drawImage(src, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    private static BufferedImage scale(BufferedImage src, double scaleFactor) {
        if (scaleFactor >= 1.0 - 1e-9) {
            return src;
        }
        int tw = Math.max(1, (int) Math.round(src.getWidth() * scaleFactor));
        int th = Math.max(1, (int) Math.round(src.getHeight() * scaleFactor));
        BufferedImage out = new BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, 0, 0, tw, th, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private byte[] writeJpeg(BufferedImage img, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("JPEG writer unavailable.");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(clamp01(quality));
        }
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    private static String sanitizeCompId(String compId) {
        if (compId == null) {
            return "";
        }
        String s = compId.trim().replace("..", "");
        return s.length() > 64 ? s.substring(0, 64) : s;
    }

    private static String resolveExtension(String name, String contentType) {
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot < name.length() - 1) {
                return name.substring(dot + 1).trim().toLowerCase(Locale.ROOT);
            }
        }
        if (contentType != null) {
            String ct = contentType.trim().toLowerCase(Locale.ROOT);
            if (ct.contains("png")) {
                return "png";
            }
            if (ct.contains("jpeg") || ct.contains("jpg")) {
                return "jpg";
            }
        }
        return null;
    }

    private static long parsePositiveLong(Object o, long def) {
        if (o == null) {
            return def;
        }
        try {
            long v = Long.parseLong(String.valueOf(o).trim());
            return v > 512 ? v : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int parsePositiveInt(Object o, int def) {
        if (o == null) {
            return def;
        }
        try {
            int v = Integer.parseInt(String.valueOf(o).trim());
            return v > 0 ? v : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static float parseQuality(Object o) {
        if (o == null) {
            return 0.92f;
        }
        try {
            return Float.parseFloat(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return 0.92f;
        }
    }

    private static float clamp01(float v) {
        if (v > 1f) {
            return 1f;
        }
        return Math.max(0.05f, v);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String basenameForAudit(String url) {
        if (url == null || url.isBlank()) {
            return "(없음)";
        }
        int i = url.replace('\\', '/').lastIndexOf('/');
        return i >= 0 ? url.substring(i + 1) : url;
    }
}
