package app.sharehub.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("proxy_subscriptions")
public class ProxySubscriptionEntity {
    @TableId
    private Long membershipId;
    private String subscriptionUrlEncrypted;
    private Long trafficUsedBytes;
    private LocalDateTime accessRevokedAt;
    private LocalDateTime lastSyncedAt;

    public Long getMembershipId() { return membershipId; }
    public void setMembershipId(Long membershipId) { this.membershipId = membershipId; }
    public String getSubscriptionUrlEncrypted() { return subscriptionUrlEncrypted; }
    public void setSubscriptionUrlEncrypted(String subscriptionUrlEncrypted) { this.subscriptionUrlEncrypted = subscriptionUrlEncrypted; }
    public Long getTrafficUsedBytes() { return trafficUsedBytes; }
    public void setTrafficUsedBytes(Long trafficUsedBytes) { this.trafficUsedBytes = trafficUsedBytes; }
    public LocalDateTime getAccessRevokedAt() { return accessRevokedAt; }
    public void setAccessRevokedAt(LocalDateTime accessRevokedAt) { this.accessRevokedAt = accessRevokedAt; }
    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}
