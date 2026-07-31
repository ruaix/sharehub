package app.sharehub.web;

import app.sharehub.security.ShareHubPrincipal;
import app.sharehub.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class SessionController {
    private final AuditService audit;
    public SessionController(AuditService audit) { this.audit = audit; }

    @PostMapping("/logout")
    public Map<String, String> logout(Authentication authentication, HttpServletRequest request) {
        ShareHubPrincipal principal = (ShareHubPrincipal) authentication.getPrincipal();
        audit.record(principal.id(), "USER_LOGOUT", "USER", principal.id());
        request.getSession(false).invalidate();
        return Map.of("message", "已退出登录");
    }
}
