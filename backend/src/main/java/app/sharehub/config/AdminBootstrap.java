package app.sharehub.config;

import app.sharehub.domain.UserEntity;
import app.sharehub.mapper.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserMapper users;
    private final PasswordEncoder passwords;
    private final ShareHubProperties properties;

    public AdminBootstrap(UserMapper users, PasswordEncoder passwords, ShareHubProperties properties) {
        this.users = users; this.passwords = passwords; this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        ShareHubProperties.Admin admin = properties.admin();
        if (admin == null || admin.email() == null || admin.password() == null || admin.password().length() < 12)
            throw new IllegalStateException("必须设置 ADMIN_EMAIL 和至少 12 位的 ADMIN_PASSWORD");
        String email = admin.email().trim().toLowerCase(Locale.ROOT);
        if (users.selectCount(Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getEmail, email)) > 0) return;
        UserEntity user = new UserEntity();
        user.setName(admin.name());
        user.setEmail(email);
        user.setPasswordHash(passwords.encode(admin.password()));
        user.setRole("ADMIN");
        user.setStatus("ACTIVE");
        users.insert(user);
    }
}
