package com.xngl.web.dto.contract;

import java.time.LocalDate;
import lombok.Data;

@Data
public class ContractExportRequestDto {

  private String contractType;
  private String contractStatus;
  private String keyword;
  private Long projectId;
  private Long siteId;
  private LocalDate startDate;
  private LocalDate endDate;
  private String exportType;
}
