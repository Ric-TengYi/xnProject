package com.xngl.web.dto.contract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalActionDto {

  private String action;
  private String reason;
  private String comment;
}
