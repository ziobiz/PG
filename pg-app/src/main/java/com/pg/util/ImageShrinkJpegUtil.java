package com.pg.util;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * 서버 저장용 이미지 자동 축소: 원본이 PNG/JPG여도 최종은 JPEG로 재인코딩하여 목표 바이트 이하로 맞춥니다.
 * - 원본 바이트가 크더라도(예: 40MB) 처리 가능(메모리 한도 내)
 * - 최대 변(edge)과 품질을 단계적으로 낮춥니다.
 */
public final class ImageShrinkJpegUtil {

    private ImageShrinkJpegUtil() {
    }

    public static byte[] optimizeToJpegUnderCap(byte[] original, long targetMaxBytes, int maxEdgePx) throws IOException {
        if (original == null || original.length == 0) {
            throw new IllegalArgumentException("이미지가 비어 있습니다.");
        }
        BufferedImage read = ImageIO.read(new ByteArrayInputStream(original));
        if (read == null) {
            throw new IllegalArgumentException("이미지를 읽을 수 없습니다.");
        }
        BufferedImage rgb = flattenToRgb(read);
        int w = rgb.getWidth();
        int h = rgb.getHeight();
        double edgeScale = w > 0 && h > 0 ? Math.min(1.0, (double) maxEdgePx / Math.max(w, h)) : 1.0;
        BufferedImage working = edgeScale >= 1.0 - 1e-9 ? rgb : scale(rgb, edgeScale);

        float startQ = 0.9f;
        float q = startQ;
        double extraScale = 1.0;
        byte[] best = writeJpeg(working, q);
        int guard = 0;
        while (best.length > targetMaxBytes && guard++ < 22) {
            if (q > 0.52f) {
                q = Math.max(0.52f, q - 0.065f);
            } else {
                extraScale *= 0.87;
                if (extraScale < 0.25) {
                    break;
                }
                working = scale(rgb, edgeScale * extraScale);
                q = startQ;
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

    private static byte[] writeJpeg(BufferedImage img, float quality) throws IOException {
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

    private static float clamp01(float v) {
        if (v > 1f) return 1f;
        return Math.max(0.05f, v);
    }
}

