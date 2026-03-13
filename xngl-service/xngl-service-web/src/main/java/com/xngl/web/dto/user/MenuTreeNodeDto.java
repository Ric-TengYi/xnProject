package com.xngl.web.dto.user;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuTreeNodeDto {

  private String id;
  private String menuCode;
  private String menuName;
  private String parentId;
  private String menuType;
  private String path;
  private String icon;
  private Integer sortOrder;
  private String visible;
  private String status;
  private Integer childCount;
  private List<MenuTreeNodeDto> children;
}
