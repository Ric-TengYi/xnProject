package com.xngl.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 登录日志表 */
@Data
@TableName("sys_login_log")
public class LoginLog {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long tenantId;
  private Long userId;
  private String username;
  private String tenantNameSnapshot;
  private String loginType;
  private Integer successFlag;
  @TableField(exist = false)
  private Boolean success; // 别名
  private String ip;
  private String userAgent;
  private String deviceFingerprint;
  private String failReason;
  private LocalDateTime loginTime;
}
