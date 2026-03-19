package com.xngl.web.dto.project;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProjectDetailDto extends ProjectListItemDto {

  private String paymentStatusLabel;
}
