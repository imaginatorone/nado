package com.solarl.nado.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * генерация изображения с текстом для проверки что пользователь — человек.
 * хранит коды в памяти с автоматической очисткой через 5 минут.
 */
@Service
public class CaptchaService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 5;
    private static final int WIDTH = 180;
    private static final int HEIGHT = 60;
    private static final long TTL_MS = 5 * 60 * 1000;

    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    public Map<String, String> generateChallenge() {
        cleanup();

        String code = generateCode();
        String id = UUID.randomUUID().toString();
        String imageBase64 = renderImage(code);

        store.put(id, new CaptchaEntry(code, System.currentTimeMillis()));

        return Map.of(
                "id", id,
                "image", "data:image/png;base64," + imageBase64
        );
    }

    public boolean verify(String id, String userInput) {
        if (id == null || userInput == null) return false;

        CaptchaEntry entry = store.remove(id);
        if (entry == null) return false;
        if (System.currentTimeMillis() - entry.createdAt > TTL_MS) return false;

        return entry.code.equalsIgnoreCase(userInput.trim());
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private String renderImage(String code) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(245, 245, 245));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(
                    180 + random.nextInt(60),
                    180 + random.nextInt(60),
                    180 + random.nextInt(60)
            ));
            g.setStroke(new BasicStroke(1 + random.nextFloat()));
            g.drawLine(
                    random.nextInt(WIDTH), random.nextInt(HEIGHT),
                    random.nextInt(WIDTH), random.nextInt(HEIGHT)
            );
        }

        for (int i = 0; i < 80; i++) {
            g.setColor(new Color(
                    160 + random.nextInt(80),
                    160 + random.nextInt(80),
                    160 + random.nextInt(80)
            ));
            g.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), 2, 2);
        }

        Font font = new Font("SansSerif", Font.BOLD, 28 + random.nextInt(6));
        g.setFont(font);

        int charWidth = WIDTH / (CODE_LENGTH + 1);
        for (int i = 0; i < code.length(); i++) {
            AffineTransform orig = g.getTransform();
            double angle = (random.nextDouble() - 0.5) * 0.5;
            int x = 12 + i * charWidth;
            int y = 35 + random.nextInt(12);
            g.rotate(angle, x, y);

            g.setColor(new Color(
                    random.nextInt(80),
                    random.nextInt(80),
                    80 + random.nextInt(100)
            ));
            g.drawString(String.valueOf(code.charAt(i)), x, y);
            g.setTransform(orig);
        }

        g.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("ошибка генерации изображения капчи", e);
        }
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> now - e.getValue().createdAt > TTL_MS);
    }

    private static class CaptchaEntry {
        final String code;
        final long createdAt;

        CaptchaEntry(String code, long createdAt) {
            this.code = code;
            this.createdAt = createdAt;
        }
    }
}
