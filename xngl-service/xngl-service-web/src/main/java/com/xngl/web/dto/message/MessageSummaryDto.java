package com.xngl.web.dto.message;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageSummaryDto {

  private long total;
  private long unread;
  private long read;
}
