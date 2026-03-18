package com.xngl.web.dto.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractCreateDto {

  private String contractNo;

  @NotBlank(message = "合同类型不能为空")
  private String contractType;

  @NotBlank(message = "合同名称不能为空")
  private String name;

  @NotBlank(message = "项目不能为空")
  private String projectId;

  @NotBlank(message = "站点不能为空")
  private String siteId;

  @NotBlank(message = "施工单位不能为空")
  private String constructionOrgId;

  @NotBlank(message = "运输单位不能为空")
  private String transportOrgId;

  private String siteOperatorOrgId;

  @NotNull(message = "签订日期不能为空")
  private String signDate;

  @NotNull(message = "生效日期不能为空")
  private String effectiveDate;

  @NotNull(message = "到期日期不能为空")
  private String expireDate;

  @NotNull(message = "约定方量不能为空")
  private BigDecimal agreedVolume;

  @NotNull(message = "单价不能为空")
  private BigDecimal unitPrice;

  @NotNull(message = "合同金额不能为空")
  private BigDecimal contractAmount;

  private Boolean isThreeParty;

  private BigDecimal unitPriceInside;

  private BigDecimal unitPriceOutside;

  private String sourceType;

  private String remark;
}
