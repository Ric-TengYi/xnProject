package com.xngl.web.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuDetailDto {

  private String id;
  private String tenantId;
  private String parentId;
  private String menuCode;
  private String menuName;
  private String menuType;
  private String path;
  private String component;
  private String icon;
  private Integer sortOrder;
  private String visible;
  private String status;
}
