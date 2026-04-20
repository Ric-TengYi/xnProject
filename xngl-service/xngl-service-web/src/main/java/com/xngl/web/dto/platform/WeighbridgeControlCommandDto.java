package com.xngl.web.dto.platform;

import lombok.Data;

@Data
public class WeighbridgeControlCommandDto {

  private Long siteId;
  private Long deviceId;
  private String command;
  private String remark;
}
