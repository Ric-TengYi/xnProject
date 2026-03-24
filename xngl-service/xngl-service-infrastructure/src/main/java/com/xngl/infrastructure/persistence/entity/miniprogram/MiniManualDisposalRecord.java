package com.xngl.infrastructure.persistence.entity.miniprogram;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mini_manual_disposal_record")
public class MiniManualDisposalRecord extends BaseEntity {

  private Long tenantId;
  private Long siteId;
  private Long contractId;
  private Long projectId;
  private Long vehicleId;
  private Long userId;
  private String reporterName;
  private String plateNo;
  private LocalDateTime disposalTime;
  private BigDecimal volume;
  private BigDecimal amount;
  private BigDecimal weightTons;
  private String photoUrls;
  private String remark;
  private String status;
  private Long ticketId;
  private String sourceChannel;
}
