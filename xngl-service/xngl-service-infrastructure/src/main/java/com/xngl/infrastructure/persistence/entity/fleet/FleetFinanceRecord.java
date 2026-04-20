package com.xngl.infrastructure.persistence.entity.fleet;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_fleet_finance_record")
public class FleetFinanceRecord extends BaseEntity {

  private Long tenantId;
  private Long fleetId;
  private Long orgId;
  private String recordNo;
  private String contractNo;
  private String statementMonth;
  private BigDecimal revenueAmount;
  private BigDecimal costAmount;
  private BigDecimal otherAmount;
  private BigDecimal settledAmount;
  private String status;
  private String remark;
}
