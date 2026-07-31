package app.sharehub.service;

import app.sharehub.config.ShareHubProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class CaptchaService {
    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private final StringRedisTemplate redis;
    private final byte[] secret;
    private final SecureRandom random = new SecureRandom();

    public CaptchaService(StringRedisTemplate redis, ShareHubProperties properties) {
        this.redis = redis;
        this.secret = properties.captchaSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 24) throw new IllegalStateException("CAPTCHA_SECRET 至少需要 24 个字符");
    }

    public Captcha create() {
        String id = UUID.randomUUID().toString().replace("-", "");
        StringBuilder answer = new StringBuilder();
        for (int i = 0; i < 5; i++) answer.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        redis.opsForValue().set("sharehub:captcha:" + id, digest(id, answer.toString()), Duration.ofMinutes(5));
        StringBuilder chars = new StringBuilder();
        for (int i = 0; i < answer.length(); i++) {
            int x = 24 + i * 32 + random.nextInt(-3, 4);
            int y = 42 + random.nextInt(-4, 5);
            int rotate = random.nextInt(-18, 19);
            chars.append("<text x=\"").append(x).append("\" y=\"").append(y).append("\" transform=\"rotate(")
                    .append(rotate).append(' ').append(x).append(' ').append(y).append(")\">")
                    .append(answer.charAt(i)).append("</text>");
        }
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"190\" height=\"60\" viewBox=\"0 0 190 60\">"
                + "<rect width=\"190\" height=\"60\" rx=\"9\" fill=\"#edf7f3\"/>"
                + "<path d=\"M0 18 Q90 54 190 15 M0 45 Q95 3 190 47\" fill=\"none\" stroke=\"#73bca6\" opacity=\".55\"/>"
                + "<g fill=\"#24765e\" font-family=\"Arial,sans-serif\" font-size=\"27\" font-weight=\"700\" letter-spacing=\"4\">"
                + chars + "</g></svg>";
        return new Captcha(id, "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8)), 300);
    }

    public boolean consume(String id, String answer) {
        if (id == null || answer == null || id.length() > 64 || answer.length() > 12) return false;
        String stored = redis.opsForValue().getAndDelete("sharehub:captcha:" + id);
        return stored != null && constantTimeEquals(stored, digest(id, answer.trim().toUpperCase()));
    }

    private String digest(String id, String answer) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((id + ":" + answer).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("验证码摘要失败", ex);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        return java.security.MessageDigest.isEqual(left.getBytes(StandardCharsets.US_ASCII), right.getBytes(StandardCharsets.US_ASCII));
    }

    public record Captcha(String id, String image, int expiresIn) {}
}
