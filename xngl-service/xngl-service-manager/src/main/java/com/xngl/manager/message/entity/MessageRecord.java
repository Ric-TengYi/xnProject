package com.xngl.manager.message.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageRecord {
    private Long id;
    private String title;
    private String content;
    private String receiverType;
    private Long receiverId;
    private String channel;
    private String status;
    private LocalDateTime sendTime;
    private LocalDateTime createTime;
}