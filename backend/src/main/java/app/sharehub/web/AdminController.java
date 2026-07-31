package app.sharehub.web;

import app.sharehub.domain.UserEntity;
import app.sharehub.mapper.UserMapper;
import app.sharehub.security.ShareHubPrincipal;
import app.sharehub.service.AuditService;
import app.sharehub.service.RegistrationService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserMapper users;
    private final RegistrationService registration;
    private final AuditService audit;

    public AdminController(UserMapper users, RegistrationService registration, AuditService audit) {
        this.users = users; this.registration = registration; this.audit = audit;
    }

    @GetMapping("/registrations")
    public List<RegistrationView> registrations() {
        return users.selectList(Wrappers.<UserEntity>lambdaQuery()
                        .eq(UserEntity::getStatus, "PENDING")
                        .orderByAsc(UserEntity::getCreatedAt)
                        .last("LIMIT 100"))
                .stream().map(user -> new RegistrationView(user.getId(), user.getName(), maskEmail(user.getEmail()), user.getCreatedAt())).toList();
    }

    @GetMapping("/users")
    public List<UserView> activeUsers() {
        return users.selectList(Wrappers.<UserEntity>lambdaQuery()
                        .eq(UserEntity::getStatus, "ACTIVE")
                        .orderByAsc(UserEntity::getName)
                        .last("LIMIT 500"))
                .stream().map(user -> new UserView(user.getId(), user.getName(), maskEmail(user.getEmail()),
                        user.getRole(), user.getStatus(), user.getCreatedAt())).toList();
    }

    @PostMapping("/registrations/{id}/approve")
    @Transactional
    public Map<String, String> approve(@PathVariable Long id, Authentication authentication) {
        return review(id, true, principal(authentication));
    }

    @PostMapping("/registrations/{id}/reject")
    @Transactional
    public Map<String, String> reject(@PathVariable Long id, Authentication authentication) {
        return review(id, false, principal(authentication));
    }

    @GetMapping("/settings")
    public Map<String, Object> settings() {
        return Map.of("registrationEnabled", registration.enabled());
    }

    @PatchMapping("/settings/registration")
    public Map<String, Object> registration(@Valid @RequestBody RegistrationSwitch body, Authentication authentication) {
        ShareHubPrincipal admin = principal(authentication);
        registration.setEnabled(body.enabled(), admin.id());
        audit.record(admin.id(), body.enabled() ? "REGISTRATION_ENABLED" : "REGISTRATION_DISABLED", "SYSTEM_SETTING", null);
        return Map.of("registrationEnabled", body.enabled(), "message", body.enabled() ? "已开放注册" : "已关闭注册");
    }

    private Map<String, String> review(Long id, boolean approve, ShareHubPrincipal admin) {
        UserEntity user = users.selectById(id);
        if (user == null || !"PENDING".equals(user.getStatus()))
            throw new ApiException(HttpStatus.NOT_FOUND, "REGISTRATION_NOT_FOUND", "申请不存在或已处理");
        user.setStatus(approve ? "ACTIVE" : "REJECTED");
        user.setApprovedBy(admin.id());
        user.setApprovedAt(LocalDateTime.now());
        users.updateById(user);
        audit.record(admin.id(), approve ? "REGISTRATION_APPROVED" : "REGISTRATION_REJECTED", "USER", id);
        return Map.of("message", approve ? "已通过申请" : "已拒绝申请");
    }

    private static ShareHubPrincipal principal(Authentication authentication) {
        return (ShareHubPrincipal) authentication.getPrincipal();
    }
    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        return email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
    }
    public record RegistrationView(Long id, String name, String email, LocalDateTime createdAt) {}
    public record UserView(Long id, String name, String email, String role, String status, LocalDateTime createdAt) {}
    public record RegistrationSwitch(@NotNull Boolean enabled) {}
}
