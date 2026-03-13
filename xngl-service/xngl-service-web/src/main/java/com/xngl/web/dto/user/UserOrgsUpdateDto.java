package com.xngl.web.dto.user;

import java.util.List;
import lombok.Data;

@Data
public class UserOrgsUpdateDto {

  private String mainOrgId;
  private List<String> orgIds;
}
