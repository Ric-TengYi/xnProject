package com.xngl.web.dto.contract;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementLineDto {

  private String id;
  private String sourceRecordType;
  private String sourceRecordId;
  private String projectId;
  private String siteId;
  private String vehicleId;
  private String bizDate;
  private BigDecimal volume;
  private BigDecimal unitPrice;
  private BigDecimal amount;
  private String remark;
}
