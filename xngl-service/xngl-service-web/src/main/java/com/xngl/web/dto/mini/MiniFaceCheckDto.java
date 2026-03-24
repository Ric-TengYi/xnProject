package com.xngl.web.dto.mini;

import lombok.Data;

@Data
public class MiniFaceCheckDto {

  private String imageUrl;
  private Boolean passed;
  private String remark;
}
