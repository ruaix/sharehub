package app.sharehub.web;

import app.sharehub.domain.UserEntity;
import app.sharehub.mapper.UserMapper;
import app.sharehub.security.ShareHubPrincipal;
import app.sharehub.service.AuditService;
import app.sharehub.service.CaptchaService;
import app.sharehub.service.RateLimitService;
import app.sharehub.service.RegistrationService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final UserMapper users;
    private final PasswordEncoder passwords;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContexts;
    private final CaptchaService captchas;
    private final RateLimitService limits;
    private final RegistrationService registration;
    private final AuditService audit;

    public AuthController(UserMapper users, PasswordEncoder passwords, AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContexts, CaptchaService captchas, RateLimitService limits,
                          RegistrationService registration, AuditService audit) {
        this.users = users; this.passwords = passwords; this.authenticationManager = authenticationManager;
        this.securityContexts = securityContexts; this.captchas = captchas; this.limits = limits;
        this.registration = registration; this.audit = audit;
    }

    @GetMapping("/health") public Map<String, Boolean> health() { return Map.of("ok", true); }
    @GetMapping("/auth/captcha")
    public CaptchaService.Captcha captcha(HttpServletRequest request) {
        if (!limits.allow("captcha", request.getRemoteAddr(), 20, Duration.ofMinutes(1)))
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "请求过于频繁，请稍后重试");
        return captchas.create();
    }
    @GetMapping("/public/registration-status")
    public Map<String, Boolean> registrationStatus() { return Map.of("enabled", registration.enabled()); }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> register(@Valid @RequestBody RegisterRequest body, HttpServletRequest request) {
        if (!registration.enabled()) throw new ApiException(HttpStatus.FORBIDDEN, "REGISTRATION_CLOSED", "当前暂未开放注册");
        if (!limits.allow("register", request.getRemoteAddr(), 5, Duration.ofHours(1)))
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "请求过于频繁，请稍后重试");
        String email = normalize(body.email());
        UserEntity exists = users.selectOne(Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getEmail, email));
        if (exists == null) {
            UserEntity user = new UserEntity();
            user.setName(body.name().trim());
            user.setEmail(email);
            user.setPasswordHash(passwords.encode(body.password()));
            user.setRole("MEMBER");
            user.setStatus("PENDING");
            users.insert(user);
            audit.record(null, "USER_REGISTERED", "USER", user.getId());
        } else {
            passwords.matches(body.password(), "$2a$12$abcdefghijklmnopqrstuuEJgV5R5wN6.D3p9IuU3c2p6k8vR/xu");
        }
        return Map.of("message", "申请已提交，审核结果将由管理员通知");
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
                               HttpServletResponse response, CsrfToken csrfToken) {
        String email = normalize(body.email());
        if (!limits.allow("login-ip", request.getRemoteAddr(), 20, Duration.ofMinutes(15))
                || !limits.allow("login-account", email, 5, Duration.ofMinutes(15)))
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "请求过于频繁，请稍后重试");
        if (!captchas.consume(body.captchaId(), body.captchaAnswer()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "CAPTCHA_INVALID", "验证码错误或已过期");
        if (limits.isLocked(email))
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "ACCOUNT_LOCKED", "登录暂时锁定，请稍后重试");
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, body.password()));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            request.getSession(true);
            request.changeSessionId();
            securityContexts.saveContext(context, request, response);
            limits.clearLoginFailures(email);
            ShareHubPrincipal principal = (ShareHubPrincipal) authentication.getPrincipal();
            audit.record(principal.id(), "USER_LOGIN", "USER", principal.id());
            return new LoginResponse(new UserView(principal.id(), principal.name(), maskEmail(principal.email()), principal.role()), csrfToken.getToken());
        } catch (RuntimeException ex) {
            limits.recordLoginFailure(email);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "邮箱或密码错误");
        }
    }

    @GetMapping("/auth/me")
    public LoginResponse me(Authentication authentication, CsrfToken csrfToken) {
        ShareHubPrincipal principal = (ShareHubPrincipal) authentication.getPrincipal();
        return new LoginResponse(new UserView(principal.id(), principal.name(), maskEmail(principal.email()), principal.role()), csrfToken.getToken());
    }

    private static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        return email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 40) @Pattern(regexp = "^[^<>\\p{Cntrl}]+$") String name,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 10, max = 128)
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$") String password) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank @Size(max = 128) String password,
                               @NotBlank String captchaId, @NotBlank @Size(min = 5, max = 5) String captchaAnswer) {}
    public record UserView(Long id, String name, String email, String role) {}
    public record LoginResponse(UserView user, String csrfToken) {}
}
