package com.xngl.web.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogListItemDto {

  private String id;
  private String userId;
  private String username;
  private String module;
  private String operation;
  private String method;
  private String requestUri;
  private String ip;
  private Long durationMs;
  private String createTime;
}
