package com.xngl.infrastructure.persistence.entity.project;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_project_payment_record")
public class ProjectPaymentRecord extends BaseEntity {

  private Long tenantId;
  private Long projectId;
  private String paymentNo;
  private String paymentType;
  private BigDecimal amount;
  private LocalDate paymentDate;
  private String voucherNo;
  private String status;
  private String sourceType;
  private Long sourceId;
  private String remark;
  private Long operatorId;
  private Long cancelOperatorId;
  private LocalDateTime cancelTime;
  private String cancelReason;
}
