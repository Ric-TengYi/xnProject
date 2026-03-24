package com.xngl.manager.message;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.manager.message.entity.MessageRecord;
import java.time.LocalDateTime;
import java.util.List;

public interface MessageRecordService {

  IPage<MessageRecord> pageForUser(
      Long tenantId,
      Long userId,
      String keyword,
      String status,
      LocalDateTime startTime,
      LocalDateTime endTime,
      int pageNo,
      int pageSize);

  List<MessageRecord> listForUser(
      Long tenantId,
      Long userId,
      String keyword,
      String status,
      LocalDateTime startTime,
      LocalDateTime endTime);

  MessageRecord markRead(Long tenantId, Long userId, Long id);

  int markAllRead(Long tenantId, Long userId);

  MessageSummary getSummary(Long tenantId, Long userId);

  void pushApprovalResult(ApprovalMessageCommand command);

  void pushUserMessage(
      Long tenantId,
      Long receiverId,
      String title,
      String content,
      String category,
      String linkUrl,
      String bizType,
      String bizId,
      String senderName);

  record MessageSummary(long total, long unread, long read) {}
}
