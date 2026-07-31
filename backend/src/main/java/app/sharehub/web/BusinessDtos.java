package app.sharehub.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public final class BusinessDtos {
    private BusinessDtos() {}

    public record ProxyRequest(
            @Size(max = 1000) @Pattern(regexp = "^https?://[^\\s<>]+$") String panelUrl,
            @Size(max = 1000) @Pattern(regexp = "^https?://[^\\s<>]+$") String probeUrl,
            @NotNull @Min(0) @Max(100000) Integer nodeTotal,
            @Min(1) @Max(1000000) Integer trafficLimitGb,
            @Min(1) @Max(10000) Integer deviceLimit) {}

    public record CreateServiceRequest(
            @NotBlank @Size(max = 80) @Pattern(regexp = "^[^<>\\p{Cntrl}]+$") String name,
            @NotBlank @Pattern(regexp = "^(STREAMING|PROXY|SUBSCRIPTION|AI|OTHER)$") String category,
            @Size(max = 200) String accountName,
            @Size(max = 1000) String secret,
            @NotNull @Min(1) @Max(10000) Integer seatTotal,
            @NotNull @Min(0) @Max(100000000) Integer monthlyPriceCents,
            @FutureOrPresent LocalDateTime renewAt,
            @Size(max = 1000) String notes,
            @Valid ProxyRequest proxy) {}

    public record AssignMembershipRequest(
            @NotNull @Positive Long userId,
            @NotNull @Positive Long serviceId,
            @NotNull @PastOrPresent LocalDateTime startedAt,
            @NotNull @Future LocalDateTime expiresAt,
            @NotNull @Min(0) @Max(100000000) Integer priceCents,
            @Size(max = 2000) @Pattern(regexp = "^https?://[^\\s<>]+$") String subscriptionUrl,
            @Size(max = 500) String note) {}

    public record RenewRequest(
            @NotNull @Future LocalDateTime expiresAt,
            @NotNull @Min(0) @Max(100000000) Integer priceCents,
            @Size(max = 500) String note) {}

    public record ServiceView(Long id, String name, String category, Integer seatTotal, Integer seatUsed,
                              Integer monthlyPriceCents, LocalDateTime renewAt, String status, ProxyView proxy) {}
    public record ProxyView(String probeUrl, Integer nodeTotal, Integer trafficLimitGb, Integer deviceLimit) {}
    public record MembershipView(Long id, Long userId, String userName, String userEmail, Long serviceId,
                                 String serviceName, String category, LocalDateTime startedAt,
                                 LocalDateTime expiresAt, Integer priceCents, String status) {}
    public record AccessView(String accountName, String secret, String probeUrl,
                             String panelUrl, String subscriptionUrl) {}
    public record OrderView(Long id, Long userId, Long serviceId, Long membershipId, String type,
                            Integer amountCents, String status, LocalDateTime periodStart,
                            LocalDateTime periodEnd, String note, LocalDateTime createdAt) {}
}
