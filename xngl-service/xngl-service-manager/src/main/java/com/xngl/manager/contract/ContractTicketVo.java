package com.xngl.manager.contract;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractTicketVo {
  private Long id;
  private Long contractId;
  private String ticketNo;
  private String ticketType;
  private LocalDate ticketDate;
  private BigDecimal amount;
  private BigDecimal volume;
  private String status;
  private String remark;
  private Long creatorId;
  private String creatorName;
  private LocalDateTime createTime;
}