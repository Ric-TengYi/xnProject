package com.xngl.web.dto.project;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class ProjectPaymentCreateDto {

  private String paymentNo;
  private String paymentType;

  @NotNull(message = "交款金额不能为空")
  @DecimalMin(value = "0.01", message = "交款金额必须大于 0")
  private BigDecimal amount;

  @NotNull(message = "交款日期不能为空")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate paymentDate;

  private String voucherNo;
  private String sourceType;
  private String sourceId;
  private String remark;
}
