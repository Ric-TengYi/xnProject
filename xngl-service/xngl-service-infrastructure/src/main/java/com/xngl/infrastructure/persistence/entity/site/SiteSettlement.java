package com.xngl.infrastructure.persistence.entity.site;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_site_settlement")
public class SiteSettlement extends BaseEntity {

  private Long siteId;
  private String settlementNo;
  private LocalDate periodStart;
  private LocalDate periodEnd;
  private LocalDate settlementDate;
  private BigDecimal totalVolume;
  private BigDecimal unitPrice;
  private BigDecimal totalAmount;
  private BigDecimal adjustAmount;
  private BigDecimal payableAmount;
  private String settlementStatus;
  private String approvalStatus;
  private String remark;
}
