package app.sharehub.web;

import app.sharehub.security.ShareHubPrincipal;
import app.sharehub.service.BusinessService;
import app.sharehub.web.BusinessDtos.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api")
public class BusinessController {
    private final BusinessService business;

    public BusinessController(BusinessService business) {
        this.business = business;
    }

    @GetMapping("/services")
    public List<ServiceView> services(Authentication authentication) {
        ShareHubPrincipal user = principal(authentication);
        return business.listServices(user.id(), "ADMIN".equals(user.role()));
    }

    @GetMapping("/memberships")
    public List<MembershipView> myMemberships(Authentication authentication) {
        ShareHubPrincipal user = principal(authentication);
        return business.listMemberships(user.id(), false);
    }

    @GetMapping("/memberships/{id}/access")
    public AccessView access(@PathVariable @Positive Long id, Authentication authentication) {
        ShareHubPrincipal user = principal(authentication);
        return business.access(id, user.id(), "ADMIN".equals(user.role()));
    }

    @GetMapping("/orders")
    public List<OrderView> myOrders(Authentication authentication) {
        return business.listOrders(principal(authentication).id());
    }

    @PostMapping("/admin/services")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceView createService(@Valid @RequestBody CreateServiceRequest body, Authentication authentication) {
        return business.createService(body, principal(authentication).id());
    }

    @DeleteMapping("/admin/services/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> archiveService(@PathVariable @Positive Long id, Authentication authentication) {
        business.archiveService(id, principal(authentication).id());
        return Map.of("message", "项目已归档");
    }

    @GetMapping("/admin/memberships")
    @PreAuthorize("hasRole('ADMIN')")
    public List<MembershipView> memberships() {
        return business.listMemberships(null, true);
    }

    @PostMapping("/admin/memberships")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public MembershipView assign(@Valid @RequestBody AssignMembershipRequest body, Authentication authentication) {
        return business.assign(body, principal(authentication).id());
    }

    @PostMapping("/admin/memberships/{id}/renew")
    @PreAuthorize("hasRole('ADMIN')")
    public MembershipView renew(@PathVariable @Positive Long id, @Valid @RequestBody RenewRequest body,
                                Authentication authentication) {
        return business.renew(id, body, principal(authentication).id());
    }

    @PostMapping("/admin/memberships/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> cancel(@PathVariable @Positive Long id, Authentication authentication) {
        business.cancel(id, principal(authentication).id());
        return Map.of("message", "已退租并撤销访问权限");
    }

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderView> orders() {
        return business.listOrders(null);
    }

    private static ShareHubPrincipal principal(Authentication authentication) {
        return (ShareHubPrincipal) authentication.getPrincipal();
    }
}
