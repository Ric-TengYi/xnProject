package com.xngl.web.dto.unit;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UnitUpsertDto {

  @NotBlank(message = "单位名称不能为空")
  private String orgName;

  @NotBlank(message = "单位类型不能为空")
  private String orgType;

  private String orgCode;
  private String contactPerson;
  private String contactPhone;
  private String address;
  private String unifiedSocialCode;
  private String remark;
  private String status;
}
