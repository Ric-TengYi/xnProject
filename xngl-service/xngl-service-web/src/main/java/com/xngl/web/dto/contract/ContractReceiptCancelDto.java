package com.xngl.web.dto.contract;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContractReceiptCancelDto {

  @Size(max = 500, message = "备注长度不能超过 500")
  private String remark;
}
