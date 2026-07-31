package app.sharehub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sharehub")
public record ShareHubProperties(
        String frontendOrigin,
        String captchaSecret,
        String ipHashSalt,
        String masterKey,
        Admin admin
) {
    public record Admin(String name, String email, String password) {}
}
