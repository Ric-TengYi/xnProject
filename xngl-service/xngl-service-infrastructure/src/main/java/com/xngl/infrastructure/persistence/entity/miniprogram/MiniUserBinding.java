package com.xngl.infrastructure.persistence.entity.miniprogram;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mini_user_binding")
public class MiniUserBinding extends BaseEntity {

  private Long tenantId;
  private Long userId;
  private String username;
  private String mobile;
  private String openId;
  private String unionId;
  private String sourceChannel;
  private LocalDateTime lastLoginTime;
  private String lastLoginIp;
  private String lastLoginDevice;
  private String status;
  private String remark;
}
