package com.xngl.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_sso_login_ticket")
public class SsoLoginTicket extends BaseEntity {

  private Long tenantId;
  private Long userId;
  private String username;
  private String ticket;
  private String targetPlatform;
  private String redirectUri;
  private LocalDateTime expiresAt;
  private Integer usedFlag;
  private LocalDateTime usedTime;
  private String extJson;
}
