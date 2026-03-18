package com.xngl.infrastructure.persistence.entity.contract;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_contract_stat_snapshot")
public class ContractStatSnapshot extends BaseEntity {

  private Long tenantId;
  private LocalDate statDate;
  private String statMonth;
  private String statType;
  private String dimensionType;
  private Long dimensionId;
  private Integer contractCount;
  private Integer newContractCount;
  private BigDecimal contractAmount;
  private BigDecimal receiptAmount;
  private BigDecimal settlementAmount;
  private BigDecimal agreedVolume;
  private BigDecimal actualVolume;
}
