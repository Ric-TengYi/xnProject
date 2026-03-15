package com.xngl.infrastructure.persistence.entity.contract;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_contract_receipt")
public class ContractReceipt extends BaseEntity {

  /** 租户隔离字段。 */
  private Long tenantId;
  /** 关联合同 ID。 */
  private Long contractId;
  /** 入账流水号。 */
  private String receiptNo;
  /** 入账日期。 */
  private LocalDate receiptDate;
  /** 入账金额，冲销流水为负数。 */
  private BigDecimal amount;
  /** 入账类型：MANUAL/REVERSAL。 */
  private String receiptType;
  /** 财务凭证号。 */
  private String voucherNo;
  /** 银行流水号。 */
  private String bankFlowNo;
  /** 流水状态：NORMAL/CANCELLED。 */
  private String status;
  /** 操作人。 */
  private Long operatorId;
  /** 备注。 */
  private String remark;
}
