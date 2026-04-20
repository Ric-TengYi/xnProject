package com.xngl.infrastructure.persistence.entity.organization;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {

  private Long tenantId;
  private String username;
  private String password;
  private String passwordHash;
  private String name;
  private String mobile;
  private String email;
  private String avatarUrl;
  private String idCardMask;
  private String userType;
  private Long mainOrgId;
  private String status;
  private LocalDateTime lastLoginTime;
  private LocalDateTime passwordExpireTime;
  private Integer needResetPassword;
  private Integer lockStatus;
  private String lockReason;
  private String authSource;
  private String externalUserId;

  // 业务方法：加密密码 setter
  public void setPasswordEncrypted(String encryptedPassword) {
    this.passwordHash = encryptedPassword;
  }
}
