package com.xngl.web.dto.user;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgTreeNodeDto {

  private String id;
  private String orgCode;
  private String orgName;
  private String parentId;
  private String leaderUserId;
  private String leaderName;
  private int childrenCount;
  private List<OrgTreeNodeDto> children;
}
