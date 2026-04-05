package com.pg.util;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 브랜딩·URL 결제 파비콘 등: 업로드 이미지를 32×32 PNG로 통일 저장.
 */
public final class FaviconImageUtil {

    private FaviconImageUtil() {
    }

    public static void saveMultipartAsFaviconPng32(MultipartFile file, Path targetPath) throws IOException {
        BufferedImage src = ImageIO.read(file.getInputStream());
        if (src == null) {
            throw new IOException("이미지 파일을 읽을 수 없습니다.");
        }
        BufferedImage out = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setComposite(AlphaComposite.Clear);
            g2.fillRect(0, 0, 32, 32);
            g2.setComposite(AlphaComposite.SrcOver);
            g2.drawImage(src, 0, 0, 32, 32, null);
        } finally {
            g2.dispose();
        }
        if (!ImageIO.write(out, "png", targetPath.toFile())) {
            throw new IOException("PNG 변환 저장에 실패했습니다.");
        }
    }
}
