package com.xngl.web.dto.message;

import lombok.Data;

@Data
public class MessageListItemDto {

  private String id;
  private String title;
  private String content;
  private String category;
  private String channel;
  private String status;
  private String statusLabel;
  private String priority;
  private String priorityLabel;
  private String linkUrl;
  private String bizType;
  private String bizId;
  private String senderName;
  private String receiverType;
  private String sendTime;
  private String readTime;
}
