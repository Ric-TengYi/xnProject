package com.xngl.manager.contract;

import lombok.Getter;

@Getter
public class ContractServiceException extends RuntimeException {

  private final int code;

  public ContractServiceException(int code, String message) {
    super(message);
    this.code = code;
  }
}
