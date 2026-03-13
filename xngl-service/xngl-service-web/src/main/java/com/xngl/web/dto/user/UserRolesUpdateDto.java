package com.xngl.web.dto.user;

import java.util.List;
import lombok.Data;

@Data
public class UserRolesUpdateDto {

  private List<String> roleIds;
}
