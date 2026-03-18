package com.xngl.infrastructure.persistence.entity.contract;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.time.LocalDate;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_contract")
public class Contract extends BaseEntity {

  private String code;
  private String name;
  private Long projectId;
  private Long partyId;
  private BigDecimal amount;
  private Integer status;

  private Long tenantId;
  private String contractNo;
  private String contractType;
  private Long siteId;
  private Long constructionOrgId;
  private Long transportOrgId;
  private Long siteOperatorOrgId;
  private LocalDate signDate;
  private LocalDate effectiveDate;
  private LocalDate expireDate;
  private BigDecimal agreedVolume;
  private BigDecimal unitPrice;
  private BigDecimal contractAmount;
  private BigDecimal receivedAmount;
  private BigDecimal settledAmount;
  private Integer changeVersion;
  private String approvalStatus;
  private String contractStatus;
  private String remark;
  private Boolean isThreeParty;
  private BigDecimal unitPriceInside;
  private BigDecimal unitPriceOutside;
  private String sourceType;
  private Long applicantId;
  private String rejectReason;
}
