package com.xngl.web.dto.contract;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ContractDetailDto extends ContractItemDto {

  private String siteOperatorOrgId;
  private String siteOperatorOrgName;
  private String partyId;
  private String remark;
  private String code;
  private BigDecimal amount;
}
