package com.xngl.infrastructure.persistence.entity.contract;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_contract_ticket")
public class ContractTicket extends BaseEntity {

  private Long tenantId;
  private Long contractId;
  private String ticketNo;
  private String ticketType;
  private LocalDate ticketDate;
  private BigDecimal amount;
  private BigDecimal volume;
  private String status;
  private String remark;
  private Long creatorId;
}