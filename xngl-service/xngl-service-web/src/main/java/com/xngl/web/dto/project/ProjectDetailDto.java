package com.xngl.web.dto.project;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProjectDetailDto extends ProjectListItemDto {

  private String paymentStatusLabel;
  private List<ProjectContractSummaryDto> contractDetails;
  private List<ProjectSiteSummaryDto> siteDetails;
  private List<ProjectPermitSummaryDto> permits;
  private ProjectConfigDto config;
}
