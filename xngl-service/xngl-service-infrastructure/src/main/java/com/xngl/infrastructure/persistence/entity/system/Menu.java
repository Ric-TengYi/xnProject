package com.xngl.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class Menu extends BaseEntity {

  private Long tenantId;
  private String tenantScope;
  private String menuCode;
  private String menuName;
  private Long parentId;
  private String menuType;
  @TableField(exist = false)
  private String path;
  private String routePath;
  @TableField(exist = false)
  private String component;
  private String componentPath;
  private String icon;
  private String permissionCode;
  private Integer sortOrder;
  @TableField(exist = false)
  private Boolean visible;
  private Integer visibleFlag;
  private Integer keepAliveFlag;
  private Integer hiddenFlag;
  private String status;

  public Long getMenuId() { return getId(); }
}
