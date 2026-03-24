package com.xngl.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_error_log")
public class ErrorLog {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long tenantId;
  private Long userId;
  private String username;
  private String level;
  private String exceptionType;
  private String errorMessage;
  private String requestUri;
  private String httpMethod;
  private String ip;
  private String stackTrace;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  private Integer deleted;
}
