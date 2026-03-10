package com.xngl.infrastructure.persistence.entity.contract;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_contract")
public class Contract extends BaseEntity {

  private String code;
  private String name;
  private Long projectId;
  private Long partyId;
  private BigDecimal amount;
  private Integer status;
}
