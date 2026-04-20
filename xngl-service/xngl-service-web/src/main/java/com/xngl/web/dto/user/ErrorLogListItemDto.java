package com.xngl.web.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorLogListItemDto {

  private String id;
  private String userId;
  private String username;
  private String level;
  private String exceptionType;
  private String errorMessage;
  private String requestUri;
  private String httpMethod;
  private String ip;
  private String stackTrace;
  private String createTime;
}
