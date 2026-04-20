package com.xngl.web.dto.project;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectListItemDto {

  private String id;
  private String code;
  private String name;
  private String address;
  private Integer status;
  private String statusLabel;
  private String orgId;
  private String orgName;
  private Long contractCount;
  private Long siteCount;
  private BigDecimal totalAmount;
  private BigDecimal paidAmount;
  private BigDecimal debtAmount;
  private String lastPaymentDate;
  private String paymentStatus;
  private String createTime;
  private String updateTime;
}
