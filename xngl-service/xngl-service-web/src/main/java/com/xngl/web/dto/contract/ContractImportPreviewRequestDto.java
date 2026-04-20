package com.xngl.web.dto.contract;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ContractImportPreviewRequestDto {

  private String fileName;

  @NotEmpty
  private List<Map<String, String>> rows;
}
