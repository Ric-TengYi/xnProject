package com.xngl.web.dto.site;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteListItemDto {

  private String id;
  private String name;
  private String code;
  private String address;
  private Integer status;
  private String siteType;
  private BigDecimal capacity;
  private String settlementMode;
}
