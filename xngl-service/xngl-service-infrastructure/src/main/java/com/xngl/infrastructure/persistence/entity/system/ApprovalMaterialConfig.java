package com.xngl.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_approval_material_config")
public class ApprovalMaterialConfig extends BaseEntity {

  private Long tenantId;
  private String processKey;
  private String materialCode;
  private String materialName;
  private String materialType;
  @TableField("required_flag")
  private Integer requiredFlag;
  private Integer sortOrder;
  private String status;
  private String remark;
}
