package com.xngl.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

  private int code;
  private String message;
  private T data;

  public static <T> ApiResult<T> ok() {
    return ok((T) null);
  }

  public static <T> ApiResult<T> ok(T data) {
    return new ApiResult<>(200, "OK", data);
  }

  public static <T> ApiResult<T> fail(int code, String message) {
    return new ApiResult<>(code, message, null);
  }
}
