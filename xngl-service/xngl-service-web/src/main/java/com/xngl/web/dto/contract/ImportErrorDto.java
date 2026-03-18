package com.xngl.web.dto.contract;

import lombok.Data;

@Data
public class ImportErrorDto {

  private String id;
  private Integer rowNo;
  private String contractNo;
  private String errorCode;
  private String errorMessage;
}
