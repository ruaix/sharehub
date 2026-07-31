package app.sharehub.service;

import app.sharehub.config.ShareHubProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;

@Service
public class RateLimitService {
    private final StringRedisTemplate redis;
    private final byte[] salt;

    public RateLimitService(StringRedisTemplate redis, ShareHubProperties properties) {
        this.redis = redis;
        this.salt = properties.ipHashSalt().getBytes(StandardCharsets.UTF_8);
    }

    public boolean allow(String scope, String value, long maximum, Duration window) {
        String key = "sharehub:limit:" + scope + ":" + digest(value);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) redis.expire(key, window);
        return count != null && count <= maximum;
    }

    public boolean isLocked(String email) {
        return Boolean.TRUE.equals(redis.hasKey(lockKey(email)));
    }

    public void recordLoginFailure(String email) {
        String failureKey = "sharehub:login-failure:" + digest(email);
        Long count = redis.opsForValue().increment(failureKey);
        if (count != null && count == 1) redis.expire(failureKey, Duration.ofMinutes(15));
        if (count != null && count >= 5) {
            redis.opsForValue().set(lockKey(email), "1", Duration.ofMinutes(15));
            redis.delete(failureKey);
        }
    }

    public void clearLoginFailures(String email) {
        redis.delete("sharehub:login-failure:" + digest(email));
    }

    public String digest(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(salt, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("安全摘要初始化失败", ex);
        }
    }

    private String lockKey(String email) { return "sharehub:account-lock:" + digest(email); }
}
