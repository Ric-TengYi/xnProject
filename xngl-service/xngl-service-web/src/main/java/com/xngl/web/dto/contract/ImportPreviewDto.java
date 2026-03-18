package com.xngl.web.dto.contract;

import java.util.List;
import lombok.Data;

@Data
public class ImportPreviewDto {

  private String batchId;
  private Integer totalCount;
  private Integer validCount;
  private Integer errorCount;
  private List<ImportErrorDto> errors;
}
