package com.xngl.web.dto.user;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataScopePreviewDto {

  private List<String> orgIds;
  private List<String> projectIds;
  private String summary;
}
