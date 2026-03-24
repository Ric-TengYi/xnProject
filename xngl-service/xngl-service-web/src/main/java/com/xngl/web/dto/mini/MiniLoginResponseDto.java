package com.xngl.web.dto.mini;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniLoginResponseDto {

  private String token;
  private String tokenType;
  private long expiresIn;
  private MiniUserProfileDto user;
  private List<MiniAccessibleSiteDto> accessibleSites;
}
