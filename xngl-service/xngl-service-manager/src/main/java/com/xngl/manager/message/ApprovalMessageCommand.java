package com.xngl.manager.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalMessageCommand {

  private Long tenantId;
  private Long receiverId;
  private String title;
  private String content;
  private String category;
  private String linkUrl;
  private String bizType;
  private String bizId;
  private String senderName;
}
