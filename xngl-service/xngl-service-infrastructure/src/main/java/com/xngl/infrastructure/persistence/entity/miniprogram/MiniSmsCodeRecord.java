package com.xngl.infrastructure.persistence.entity.miniprogram;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mini_sms_code_record")
public class MiniSmsCodeRecord extends BaseEntity {

  private Long tenantId;
  private Long userId;
  private String mobile;
  private String bizType;
  private String verifyCode;
  private LocalDateTime expiresAt;
  private Integer usedFlag;
  private LocalDateTime usedTime;
  private String sendStatus;
  private String channel;
  private String extJson;
  private String remark;
}
