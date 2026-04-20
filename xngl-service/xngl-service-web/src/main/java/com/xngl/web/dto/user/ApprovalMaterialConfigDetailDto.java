package com.xngl.web.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalMaterialConfigDetailDto {

  private String id;
  private String processKey;
  private String materialCode;
  private String materialName;
  private String materialType;
  private Boolean required;
  private Integer sortOrder;
  private String status;
  private String remark;
}
