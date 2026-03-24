package com.xngl.web.dto.mini;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAuthCodeResponseDto {

  private String mobile;
  private String expiresAt;
  private String sendStatus;
  private String mockCode;
}
