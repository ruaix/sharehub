package app.sharehub.service;

import app.sharehub.domain.*;
import app.sharehub.mapper.*;
import app.sharehub.web.ApiException;
import app.sharehub.web.BusinessDtos.*;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BusinessService {
    private final SharedServiceMapper services;
    private final MembershipMapper memberships;
    private final ProxyServiceMapper proxyServices;
    private final ProxySubscriptionMapper proxySubscriptions;
    private final OrderMapper orders;
    private final UserMapper users;
    private final FieldEncryptionService encryption;
    private final AuditService audit;

    public BusinessService(SharedServiceMapper services, MembershipMapper memberships,
                           ProxyServiceMapper proxyServices, ProxySubscriptionMapper proxySubscriptions,
                           OrderMapper orders, UserMapper users, FieldEncryptionService encryption, AuditService audit) {
        this.services = services;
        this.memberships = memberships;
        this.proxyServices = proxyServices;
        this.proxySubscriptions = proxySubscriptions;
        this.orders = orders;
        this.users = users;
        this.encryption = encryption;
        this.audit = audit;
    }

    public List<ServiceView> listServices(Long viewerId, boolean admin) {
        expireOverdue();
        List<SharedServiceEntity> rows;
        if (admin) {
            rows = services.selectList(Wrappers.<SharedServiceEntity>lambdaQuery()
                    .ne(SharedServiceEntity::getStatus, "ARCHIVED").orderByDesc(SharedServiceEntity::getCreatedAt));
        } else {
            List<Long> ids = memberships.selectList(Wrappers.<MembershipEntity>lambdaQuery()
                            .eq(MembershipEntity::getUserId, viewerId)
                            .in(MembershipEntity::getStatus, List.of("ACTIVE", "EXPIRING")))
                    .stream().filter(this::hasCurrentAccess).map(MembershipEntity::getServiceId).distinct().toList();
            if (ids.isEmpty()) return List.of();
            rows = services.selectBatchIds(ids).stream().filter(s -> "ACTIVE".equals(s.getStatus())).toList();
        }
        return rows.stream().map(this::serviceView).toList();
    }

    @Transactional
    public ServiceView createService(CreateServiceRequest body, Long adminId) {
        if ("PROXY".equals(body.category()) && body.proxy() == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "PROXY_CONFIG_REQUIRED", "梯子服务必须填写探针或套餐配置");
        if (!"PROXY".equals(body.category()) && body.proxy() != null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "PROXY_CONFIG_NOT_ALLOWED", "只有梯子服务可以填写探针配置");

        SharedServiceEntity entity = new SharedServiceEntity();
        entity.setName(body.name().trim());
        entity.setCategory(body.category());
        entity.setAccountName(blankToNull(body.accountName()));
        entity.setSecretEncrypted(encryption.encrypt(blankToNull(body.secret()), "service-secret"));
        entity.setSeatTotal(body.seatTotal());
        entity.setMonthlyPriceCents(body.monthlyPriceCents());
        entity.setRenewAt(body.renewAt());
        entity.setStatus("ACTIVE");
        entity.setNotes(blankToNull(body.notes()));
        services.insert(entity);

        if (body.proxy() != null) {
            ProxyServiceEntity proxy = new ProxyServiceEntity();
            proxy.setServiceId(entity.getId());
            proxy.setPanelUrlEncrypted(encryption.encrypt(blankToNull(body.proxy().panelUrl()), "proxy-panel"));
            proxy.setProbeUrl(blankToNull(body.proxy().probeUrl()));
            proxy.setNodeTotal(body.proxy().nodeTotal());
            proxy.setTrafficLimitGb(body.proxy().trafficLimitGb());
            proxy.setDeviceLimit(body.proxy().deviceLimit());
            proxyServices.insert(proxy);
        }
        audit.record(adminId, "SERVICE_CREATED", "SHARED_SERVICE", entity.getId());
        return serviceView(entity);
    }

    @Transactional
    public void archiveService(Long serviceId, Long adminId) {
        SharedServiceEntity service = requireService(serviceId);
        long active = memberships.selectCount(Wrappers.<MembershipEntity>lambdaQuery()
                .eq(MembershipEntity::getServiceId, serviceId)
                .in(MembershipEntity::getStatus, List.of("PENDING", "ACTIVE", "EXPIRING")));
        if (active > 0) throw new ApiException(HttpStatus.CONFLICT, "SERVICE_IN_USE", "仍有有效租户，不能归档");
        service.setStatus("ARCHIVED");
        service.setUpdatedAt(LocalDateTime.now());
        services.updateById(service);
        audit.record(adminId, "SERVICE_ARCHIVED", "SHARED_SERVICE", serviceId);
    }

    @Transactional
    public MembershipView assign(AssignMembershipRequest body, Long adminId) {
        expireOverdue();
        UserEntity user = users.selectById(body.userId());
        if (user == null || !"ACTIVE".equals(user.getStatus()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_NOT_ACTIVE", "用户不存在或尚未启用");
        SharedServiceEntity service = requireService(body.serviceId());
        if (!"ACTIVE".equals(service.getStatus()))
            throw new ApiException(HttpStatus.CONFLICT, "SERVICE_NOT_ACTIVE", "项目当前不可分配");
        if (!body.expiresAt().isAfter(body.startedAt()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", "到期时间必须晚于开始时间");
        long occupied = memberships.selectCount(Wrappers.<MembershipEntity>lambdaQuery()
                .eq(MembershipEntity::getServiceId, body.serviceId())
                .in(MembershipEntity::getStatus, List.of("PENDING", "ACTIVE", "EXPIRING")));
        if (occupied >= service.getSeatTotal())
            throw new ApiException(HttpStatus.CONFLICT, "NO_AVAILABLE_SEAT", "该项目已没有可用名额");

        MembershipEntity membership = new MembershipEntity();
        membership.setUserId(body.userId());
        membership.setServiceId(body.serviceId());
        membership.setStartedAt(body.startedAt());
        membership.setExpiresAt(body.expiresAt());
        membership.setPriceCents(body.priceCents());
        membership.setStatus("ACTIVE");
        try {
            memberships.insert(membership);
        } catch (DuplicateKeyException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "MEMBERSHIP_EXISTS", "该用户已租用此项目");
        }
        if ("PROXY".equals(service.getCategory())) {
            if (body.subscriptionUrl() == null || body.subscriptionUrl().isBlank())
                throw new ApiException(HttpStatus.BAD_REQUEST, "SUBSCRIPTION_URL_REQUIRED", "梯子租户必须填写订阅链接");
            ProxySubscriptionEntity subscription = new ProxySubscriptionEntity();
            subscription.setMembershipId(membership.getId());
            subscription.setSubscriptionUrlEncrypted(encryption.encrypt(body.subscriptionUrl().trim(), "proxy-subscription"));
            subscription.setTrafficUsedBytes(0L);
            proxySubscriptions.insert(subscription);
        } else if (body.subscriptionUrl() != null && !body.subscriptionUrl().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SUBSCRIPTION_URL_NOT_ALLOWED", "此项目类型不接受订阅链接");
        }
        createOrder(membership, "NEW", body.startedAt(), body.expiresAt(), body.priceCents(), body.note(), adminId);
        audit.record(adminId, "MEMBERSHIP_CREATED", "MEMBERSHIP", membership.getId());
        return membershipView(membership, true);
    }

    @Transactional
    public MembershipView renew(Long membershipId, RenewRequest body, Long adminId) {
        MembershipEntity membership = requireMembership(membershipId);
        if (List.of("CANCELLED", "REFUNDED").contains(membership.getStatus()))
            throw new ApiException(HttpStatus.CONFLICT, "MEMBERSHIP_ENDED", "已退租的服务不能直接续费，请重新分配");
        LocalDateTime start = membership.getExpiresAt().isAfter(LocalDateTime.now())
                ? membership.getExpiresAt() : LocalDateTime.now();
        if (!body.expiresAt().isAfter(start))
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", "新到期时间必须晚于当前租期");
        membership.setExpiresAt(body.expiresAt());
        membership.setPriceCents(body.priceCents());
        membership.setStatus("ACTIVE");
        membership.setEndedAt(null);
        membership.setAccessRevokedAt(null);
        memberships.updateById(membership);
        createOrder(membership, "RENEWAL", start, body.expiresAt(), body.priceCents(), body.note(), adminId);
        audit.record(adminId, "MEMBERSHIP_RENEWED", "MEMBERSHIP", membershipId);
        return membershipView(membership, true);
    }

    @Transactional
    public void cancel(Long membershipId, Long adminId) {
        MembershipEntity membership = requireMembership(membershipId);
        if ("CANCELLED".equals(membership.getStatus()))
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_CANCELLED", "该租期已经退租");
        LocalDateTime now = LocalDateTime.now();
        membership.setStatus("CANCELLED");
        membership.setEndedAt(now);
        membership.setAccessRevokedAt(now);
        memberships.updateById(membership);
        ProxySubscriptionEntity subscription = proxySubscriptions.selectById(membershipId);
        if (subscription != null) {
            subscription.setAccessRevokedAt(now);
            proxySubscriptions.updateById(subscription);
        }
        audit.record(adminId, "MEMBERSHIP_CANCELLED", "MEMBERSHIP", membershipId);
    }

    public List<MembershipView> listMemberships(Long userId, boolean revealUser) {
        expireOverdue();
        return memberships.selectList(Wrappers.<MembershipEntity>lambdaQuery()
                        .eq(userId != null, MembershipEntity::getUserId, userId)
                        .orderByDesc(MembershipEntity::getCreatedAt).last("LIMIT 200"))
                .stream().map(row -> membershipView(row, revealUser)).toList();
    }

    public AccessView access(Long membershipId, Long viewerId, boolean admin) {
        MembershipEntity membership = requireMembership(membershipId);
        if (!admin && !membership.getUserId().equals(viewerId))
            throw new ApiException(HttpStatus.NOT_FOUND, "MEMBERSHIP_NOT_FOUND", "租期不存在");
        if (!admin && !hasCurrentAccess(membership))
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCESS_EXPIRED", "租期已结束，敏感访问权限已撤销");
        SharedServiceEntity service = requireService(membership.getServiceId());
        ProxyServiceEntity proxy = proxyServices.selectById(service.getId());
        ProxySubscriptionEntity subscription = proxySubscriptions.selectById(membershipId);
        return new AccessView(
                service.getAccountName(),
                encryption.decrypt(service.getSecretEncrypted(), "service-secret"),
                proxy == null ? null : proxy.getProbeUrl(),
                proxy == null ? null : encryption.decrypt(proxy.getPanelUrlEncrypted(), "proxy-panel"),
                subscription == null || subscription.getAccessRevokedAt() != null ? null
                        : encryption.decrypt(subscription.getSubscriptionUrlEncrypted(), "proxy-subscription"));
    }

    public List<OrderView> listOrders(Long userId) {
        return orders.selectList(Wrappers.<OrderEntity>lambdaQuery()
                        .eq(userId != null, OrderEntity::getUserId, userId)
                        .orderByDesc(OrderEntity::getCreatedAt).last("LIMIT 200"))
                .stream().map(o -> new OrderView(o.getId(), o.getUserId(), o.getServiceId(), o.getMembershipId(),
                        o.getType(), o.getAmountCents(), o.getStatus(), o.getPeriodStart(), o.getPeriodEnd(),
                        o.getNote(), o.getCreatedAt())).toList();
    }

    private ServiceView serviceView(SharedServiceEntity service) {
        long used = memberships.selectCount(Wrappers.<MembershipEntity>lambdaQuery()
                .eq(MembershipEntity::getServiceId, service.getId())
                .in(MembershipEntity::getStatus, List.of("PENDING", "ACTIVE", "EXPIRING")));
        ProxyServiceEntity proxy = proxyServices.selectById(service.getId());
        return new ServiceView(service.getId(), service.getName(), service.getCategory(), service.getSeatTotal(),
                (int) used, service.getMonthlyPriceCents(), service.getRenewAt(), service.getStatus(),
                proxy == null ? null : new ProxyView(proxy.getProbeUrl(), proxy.getNodeTotal(),
                        proxy.getTrafficLimitGb(), proxy.getDeviceLimit()));
    }

    private MembershipView membershipView(MembershipEntity membership, boolean includeUser) {
        SharedServiceEntity service = services.selectById(membership.getServiceId());
        UserEntity user = includeUser ? users.selectById(membership.getUserId()) : null;
        String effectiveStatus = membership.getStatus();
        if ("ACTIVE".equals(effectiveStatus) && membership.getExpiresAt().isBefore(LocalDateTime.now())) effectiveStatus = "EXPIRED";
        return new MembershipView(membership.getId(), membership.getUserId(),
                user == null ? null : user.getName(), user == null ? null : maskEmail(user.getEmail()),
                membership.getServiceId(), service == null ? "已删除项目" : service.getName(),
                service == null ? "OTHER" : service.getCategory(), membership.getStartedAt(),
                membership.getExpiresAt(), membership.getPriceCents(), effectiveStatus);
    }

    private boolean hasCurrentAccess(MembershipEntity membership) {
        return membership.getAccessRevokedAt() == null
                && membership.getExpiresAt() != null
                && membership.getExpiresAt().isAfter(LocalDateTime.now())
                && List.of("ACTIVE", "EXPIRING").contains(membership.getStatus());
    }

    private SharedServiceEntity requireService(Long id) {
        SharedServiceEntity service = services.selectById(id);
        if (service == null) throw new ApiException(HttpStatus.NOT_FOUND, "SERVICE_NOT_FOUND", "项目不存在");
        return service;
    }

    private MembershipEntity requireMembership(Long id) {
        MembershipEntity membership = memberships.selectById(id);
        if (membership == null) throw new ApiException(HttpStatus.NOT_FOUND, "MEMBERSHIP_NOT_FOUND", "租期不存在");
        return membership;
    }

    private void createOrder(MembershipEntity membership, String type, LocalDateTime start,
                             LocalDateTime end, Integer amount, String note, Long adminId) {
        OrderEntity order = new OrderEntity();
        order.setUserId(membership.getUserId());
        order.setServiceId(membership.getServiceId());
        order.setMembershipId(membership.getId());
        order.setType(type);
        order.setAmountCents(amount);
        order.setStatus("PAID");
        order.setPeriodStart(start);
        order.setPeriodEnd(end);
        order.setNote(blankToNull(note));
        order.setCreatedBy(adminId);
        orders.insert(order);
    }

    private void expireOverdue() {
        LocalDateTime now = LocalDateTime.now();
        memberships.update(null, Wrappers.<MembershipEntity>lambdaUpdate()
                .set(MembershipEntity::getStatus, "EXPIRED")
                .set(MembershipEntity::getAccessRevokedAt, now)
                .lt(MembershipEntity::getExpiresAt, now)
                .in(MembershipEntity::getStatus, List.of("ACTIVE", "EXPIRING")));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        return email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
    }
}
