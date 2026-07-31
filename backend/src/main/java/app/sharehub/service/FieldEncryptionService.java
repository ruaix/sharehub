package app.sharehub.service;

import app.sharehub.config.ShareHubProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class FieldEncryptionService {
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public FieldEncryptionService(ShareHubProperties properties) {
        byte[] decoded = Base64.getDecoder().decode(properties.masterKey());
        if (decoded.length != 32) throw new IllegalStateException("SHAREHUB_MASTER_KEY 必须是 Base64 编码的 32 字节密钥");
        this.key = new SecretKeySpec(decoded, "AES");
    }

    public String encrypt(String plaintext, String purpose) {
        if (plaintext == null || plaintext.isBlank()) return null;
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            cipher.updateAAD(("sharehub:" + purpose + ":v1").getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return "v1." + Base64.getUrlEncoder().withoutPadding().encodeToString(iv) + "."
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
        } catch (Exception ex) {
            throw new IllegalStateException("敏感字段加密失败", ex);
        }
    }

    public String decrypt(String payload, String purpose) {
        if (payload == null || payload.isBlank()) return null;
        try {
            String[] parts = payload.split("\\.");
            if (parts.length != 3 || !"v1".equals(parts[0])) throw new IllegalArgumentException("密文格式错误");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Base64.getUrlDecoder().decode(parts[1])));
            cipher.updateAAD(("sharehub:" + purpose + ":v1").getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(Base64.getUrlDecoder().decode(parts[2])), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("敏感字段解密失败或数据已被篡改", ex);
        }
    }
}
