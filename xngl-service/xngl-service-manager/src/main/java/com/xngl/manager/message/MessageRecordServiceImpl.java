package com.xngl.manager.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.manager.message.entity.MessageRecord;
import com.xngl.manager.message.mapper.MessageRecordMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MessageRecordServiceImpl implements MessageRecordService {

  private static final String DEFAULT_SENDER = "系统";
  private final MessageRecordMapper messageRecordMapper;

  public MessageRecordServiceImpl(MessageRecordMapper messageRecordMapper) {
    this.messageRecordMapper = messageRecordMapper;
  }

  @Override
  public IPage<MessageRecord> pageForUser(
      Long tenantId,
      Long userId,
      String keyword,
      String status,
      LocalDateTime startTime,
      LocalDateTime endTime,
      int pageNo,
      int pageSize) {
    return messageRecordMapper.selectPage(
        new Page<>(pageNo, pageSize),
        buildUserQuery(tenantId, userId, keyword, status, startTime, endTime));
  }

  @Override
  public List<MessageRecord> listForUser(
      Long tenantId,
      Long userId,
      String keyword,
      String status,
      LocalDateTime startTime,
      LocalDateTime endTime) {
    return messageRecordMapper.selectList(
        buildUserQuery(tenantId, userId, keyword, status, startTime, endTime));
  }

  @Override
  @Transactional
  public MessageRecord markRead(Long tenantId, Long userId, Long id) {
    MessageRecord record = messageRecordMapper.selectById(id);
    if (record == null || record.getTenantId() == null || !record.getTenantId().equals(tenantId)) {
      return null;
    }
    boolean canAccess =
        ("ALL".equalsIgnoreCase(record.getReceiverType()))
            || ("USER".equalsIgnoreCase(record.getReceiverType()) && userId.equals(record.getReceiverId()));
    if (!canAccess) {
      return null;
    }
    if (!"READ".equalsIgnoreCase(record.getStatus())) {
      record.setStatus("READ");
      record.setReadTime(LocalDateTime.now());
      messageRecordMapper.updateById(record);
    }
    return messageRecordMapper.selectById(id);
  }

  @Override
  @Transactional
  public int markAllRead(Long tenantId, Long userId) {
    List<MessageRecord> rows = listForUser(tenantId, userId, null, "UNREAD", null, null);
    int updated = 0;
    for (MessageRecord row : rows) {
      row.setStatus("READ");
      row.setReadTime(LocalDateTime.now());
      updated += messageRecordMapper.updateById(row);
    }
    return updated;
  }

  @Override
  public MessageSummary getSummary(Long tenantId, Long userId) {
    LambdaQueryWrapper<MessageRecord> baseQuery = new LambdaQueryWrapper<>();
    baseQuery.eq(MessageRecord::getTenantId, tenantId)
        .and(
            wrapper ->
                wrapper.eq(MessageRecord::getReceiverType, "ALL")
                    .or()
                    .eq(MessageRecord::getReceiverId, userId));
    long total = messageRecordMapper.selectCount(baseQuery);

    LambdaQueryWrapper<MessageRecord> unreadQuery = new LambdaQueryWrapper<>();
    unreadQuery.eq(MessageRecord::getTenantId, tenantId)
        .eq(MessageRecord::getStatus, "UNREAD")
        .and(
            wrapper ->
                wrapper.eq(MessageRecord::getReceiverType, "ALL")
                    .or()
                    .eq(MessageRecord::getReceiverId, userId));
    long unread = messageRecordMapper.selectCount(unreadQuery);
    return new MessageSummary(total, unread, Math.max(0, total - unread));
  }

  @Override
  @Transactional
  public void pushApprovalResult(ApprovalMessageCommand command) {
    if (command == null || command.getTenantId() == null || command.getReceiverId() == null) {
      return;
    }
    insertMessage(command, "SYSTEM");
    insertMessage(command, "SMS");
  }

  @Override
  @Transactional
  public void pushUserMessage(
      Long tenantId,
      Long receiverId,
      String title,
      String content,
      String category,
      String linkUrl,
      String bizType,
      String bizId,
      String senderName) {
    if (tenantId == null || receiverId == null) {
      return;
    }
    insertMessage(
        new ApprovalMessageCommand(
            tenantId, receiverId, title, content, category, linkUrl, bizType, bizId, senderName),
        "SYSTEM");
  }

  private void insertMessage(ApprovalMessageCommand command, String channel) {
    MessageRecord record = new MessageRecord();
    record.setTenantId(command.getTenantId());
    record.setReceiverType("USER");
    record.setReceiverId(command.getReceiverId());
    record.setTitle(command.getTitle());
    record.setContent(command.getContent());
    record.setCategory(command.getCategory());
    record.setChannel(channel);
    record.setStatus("UNREAD");
    record.setPriority("HIGH");
    record.setLinkUrl(command.getLinkUrl());
    record.setBizType(command.getBizType());
    record.setBizId(command.getBizId());
    record.setSenderName(
        StringUtils.hasText(command.getSenderName()) ? command.getSenderName() : DEFAULT_SENDER);
    record.setSendTime(LocalDateTime.now());
    messageRecordMapper.insert(record);
  }

  private LambdaQueryWrapper<MessageRecord> buildUserQuery(
      Long tenantId,
      Long userId,
      String keyword,
      String status,
      LocalDateTime startTime,
      LocalDateTime endTime) {
    LambdaQueryWrapper<MessageRecord> query = new LambdaQueryWrapper<>();
    query.eq(MessageRecord::getTenantId, tenantId)
        .and(
            wrapper ->
                wrapper.eq(MessageRecord::getReceiverType, "ALL")
                    .or()
                    .eq(MessageRecord::getReceiverId, userId))
        .eq(
            StringUtils.hasText(status) && !"all".equalsIgnoreCase(status),
            MessageRecord::getStatus,
            status == null ? null : status.trim().toUpperCase())
        .ge(startTime != null, MessageRecord::getSendTime, startTime)
        .le(endTime != null, MessageRecord::getSendTime, endTime)
        .orderByDesc(MessageRecord::getSendTime)
        .orderByDesc(MessageRecord::getId);
    if (StringUtils.hasText(keyword)) {
      String effectiveKeyword = keyword.trim();
      query.and(
          wrapper ->
              wrapper.like(MessageRecord::getTitle, effectiveKeyword)
                  .or()
                  .like(MessageRecord::getContent, effectiveKeyword)
                  .or()
                  .like(MessageRecord::getCategory, effectiveKeyword)
                  .or()
                  .like(MessageRecord::getSenderName, effectiveKeyword));
    }
    return query;
  }
}
