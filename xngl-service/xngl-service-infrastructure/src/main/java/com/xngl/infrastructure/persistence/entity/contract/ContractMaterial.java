package com.xngl.infrastructure.persistence.entity.contract;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_contract_material")
public class ContractMaterial extends BaseEntity {

  private Long tenantId;
  private Long contractId;
  private String materialName;
  private String materialType;
  private String fileUrl;
  private Long fileSize;
  private Long uploaderId;
  private String remark;
}