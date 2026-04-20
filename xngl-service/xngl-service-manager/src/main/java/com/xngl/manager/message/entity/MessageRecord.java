package com.xngl.manager.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_message_record")
public class MessageRecord extends BaseEntity {
    private Long tenantId;
    private String receiverType;
    private Long receiverId;
    private String title;
    private String content;
    private String category;
    private String channel;
    private String status;
    private String priority;
    private String linkUrl;
    private String bizType;
    private String bizId;
    private String senderName;
    private LocalDateTime sendTime;
    private LocalDateTime readTime;
}
