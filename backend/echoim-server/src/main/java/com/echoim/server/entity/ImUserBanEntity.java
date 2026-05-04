package com.echoim.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("im_user_ban")
public class ImUserBanEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer banType;
    private String reason;
    private Integer banMinutes;
    private LocalDateTime expireAt;
    private Long bannedBy;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getBanType() { return banType; }
    public void setBanType(Integer banType) { this.banType = banType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getBanMinutes() { return banMinutes; }
    public void setBanMinutes(Integer banMinutes) { this.banMinutes = banMinutes; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
    public Long getBannedBy() { return bannedBy; }
    public void setBannedBy(Long bannedBy) { this.bannedBy = bannedBy; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
