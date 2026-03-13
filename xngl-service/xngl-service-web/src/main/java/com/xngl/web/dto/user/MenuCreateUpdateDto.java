package com.xngl.web.dto.user;

import lombok.Data;

@Data
public class MenuCreateUpdateDto {

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
}
