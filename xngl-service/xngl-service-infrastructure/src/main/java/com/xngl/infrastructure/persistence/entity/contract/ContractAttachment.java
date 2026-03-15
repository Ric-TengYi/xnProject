package com.xngl.infrastructure.persistence.entity.contract;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_contract_attachment")
public class ContractAttachment extends BaseEntity {

  private Long tenantId;
  private String bizType;
  private Long bizId;
  private String fileName;
  private String fileUrl;
  private Long fileSize;
  private String mimeType;
  private String remark;
}
