package com.xngl.web.dto.unit;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UnitDetailDto extends UnitListItemDto {

  private String remark;
}
