package com.xngl.manager.project;

import lombok.Getter;

@Getter
public class ProjectPaymentException extends RuntimeException {

  private final int code;

  public ProjectPaymentException(int code, String message) {
    super(message);
    this.code = code;
  }
}
