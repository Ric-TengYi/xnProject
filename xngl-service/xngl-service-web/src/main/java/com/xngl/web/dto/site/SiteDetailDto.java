package com.xngl.web.dto.site;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteDetailDto {

  private String id;
  private String name;
  private String code;
  private String address;
  private Long projectId;
  private Integer status;
  private Long orgId;
  private String siteType;
  private BigDecimal capacity;
  private String settlementMode;
  private BigDecimal disposalUnitPrice;
  private BigDecimal disposalFeeRate;
  private BigDecimal serviceFeeUnitPrice;
  private String createTime;
  private String updateTime;
}
