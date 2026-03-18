package com.xngl.web.dto.contract;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ImportBatchDetailDto extends ImportBatchItemDto {

  private List<ImportErrorDto> errors;
}
