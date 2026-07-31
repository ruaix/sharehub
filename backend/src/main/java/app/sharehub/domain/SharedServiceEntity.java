package app.sharehub.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("shared_services")
public class SharedServiceEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String category;
    private String accountName;
    private String secretEncrypted;
    private Integer seatTotal;
    private Integer monthlyPriceCents;
    private LocalDateTime renewAt;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public String getSecretEncrypted() { return secretEncrypted; }
    public void setSecretEncrypted(String secretEncrypted) { this.secretEncrypted = secretEncrypted; }
    public Integer getSeatTotal() { return seatTotal; }
    public void setSeatTotal(Integer seatTotal) { this.seatTotal = seatTotal; }
    public Integer getMonthlyPriceCents() { return monthlyPriceCents; }
    public void setMonthlyPriceCents(Integer monthlyPriceCents) { this.monthlyPriceCents = monthlyPriceCents; }
    public LocalDateTime getRenewAt() { return renewAt; }
    public void setRenewAt(LocalDateTime renewAt) { this.renewAt = renewAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
