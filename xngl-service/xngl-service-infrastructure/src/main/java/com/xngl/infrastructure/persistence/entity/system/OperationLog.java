package com.xngl.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_operation_log")
public class OperationLog {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long tenantId;
  private Long userId;
  private String username;
  private String module;
  private String operation;
  private String action;
  private String method;
  private String bizType;
  private String bizId;
  private String requestUri;
  private String httpMethod;
  private String resultCode;
  private String content;
  private String ip;
  private String userAgent;
  private Integer durationMs;
  private LocalDateTime createTime;
}
