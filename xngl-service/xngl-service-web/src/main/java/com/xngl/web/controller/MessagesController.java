package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.message.MessageRecordService;
import com.xngl.manager.message.entity.MessageRecord;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.message.MessageListItemDto;
import com.xngl.web.dto.message.MessageSummaryDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import com.xngl.web.support.CsvExportSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/messages")
public class MessagesController {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final MessageRecordService messageRecordService;
  private final UserContext userContext;

  public MessagesController(MessageRecordService messageRecordService, UserContext userContext) {
    this.messageRecordService = messageRecordService;
    this.userContext = userContext;
  }

  @GetMapping
  public ApiResult<PageResult<MessageListItemDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    IPage<MessageRecord> page =
        messageRecordService.pageForUser(
            currentUser.getTenantId(),
            currentUser.getId(),
            keyword,
            status,
            parseTime(startTime),
            parseTime(endTime),
            pageNo,
            pageSize);
    List<MessageListItemDto> records = page.getRecords().stream().map(this::toItem).toList();
    return ApiResult.ok(new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/summary")
  public ApiResult<MessageSummaryDto> summary(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    MessageRecordService.MessageSummary summary =
        messageRecordService.getSummary(currentUser.getTenantId(), currentUser.getId());
    return ApiResult.ok(new MessageSummaryDto(summary.total(), summary.unread(), summary.read()));
  }

  @PutMapping("/{id}/read")
  public ApiResult<MessageListItemDto> markRead(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    MessageRecord record = messageRecordService.markRead(currentUser.getTenantId(), currentUser.getId(), id);
    if (record == null) {
      throw new BizException(404, "消息不存在");
    }
    return ApiResult.ok(toItem(record));
  }

  @PutMapping("/read-all")
  public ApiResult<Map<String, Object>> markAllRead(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    int updated = messageRecordService.markAllRead(currentUser.getTenantId(), currentUser.getId());
    return ApiResult.ok(Map.of("updated", updated));
  }

  @GetMapping("/export")
  public ResponseEntity<byte[]> export(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<MessageRecord> rows =
        messageRecordService.listForUser(
            currentUser.getTenantId(),
            currentUser.getId(),
            keyword,
            status,
            parseTime(startTime),
            parseTime(endTime));
    return CsvExportSupport.csvResponse(
        "messages",
        List.of("标题", "内容", "分类", "渠道", "状态", "优先级", "发送人", "发送时间", "已读时间"),
        rows.stream()
            .map(
                row ->
                    List.of(
                        CsvExportSupport.value(row.getTitle()),
                        CsvExportSupport.value(row.getContent()),
                        CsvExportSupport.value(row.getCategory()),
                        CsvExportSupport.value(row.getChannel()),
                        resolveStatusLabel(row.getStatus()),
                        resolvePriorityLabel(row.getPriority()),
                        CsvExportSupport.value(row.getSenderName()),
                        CsvExportSupport.value(row.getSendTime()),
                        CsvExportSupport.value(row.getReadTime())))
            .toList());
  }

  private MessageListItemDto toItem(MessageRecord record) {
    MessageListItemDto dto = new MessageListItemDto();
    dto.setId(record.getId() != null ? String.valueOf(record.getId()) : null);
    dto.setTitle(record.getTitle());
    dto.setContent(record.getContent());
    dto.setCategory(record.getCategory());
    dto.setChannel(record.getChannel());
    dto.setStatus(record.getStatus());
    dto.setStatusLabel(resolveStatusLabel(record.getStatus()));
    dto.setPriority(record.getPriority());
    dto.setPriorityLabel(resolvePriorityLabel(record.getPriority()));
    dto.setLinkUrl(record.getLinkUrl());
    dto.setBizType(record.getBizType());
    dto.setBizId(record.getBizId());
    dto.setSenderName(record.getSenderName());
    dto.setReceiverType(record.getReceiverType());
    dto.setSendTime(record.getSendTime() != null ? record.getSendTime().format(ISO) : null);
    dto.setReadTime(record.getReadTime() != null ? record.getReadTime().format(ISO) : null);
    return dto;
  }

  private String resolveStatusLabel(String status) {
    if (!StringUtils.hasText(status)) {
      return "未读";
    }
    return switch (status.trim().toUpperCase()) {
      case "READ" -> "已读";
      case "ARCHIVED" -> "归档";
      default -> "未读";
    };
  }

  private String resolvePriorityLabel(String priority) {
    if (!StringUtils.hasText(priority)) {
      return "普通";
    }
    return switch (priority.trim().toUpperCase()) {
      case "HIGH" -> "高";
      case "LOW" -> "低";
      default -> "普通";
    };
  }

  private LocalDateTime parseTime(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return LocalDateTime.parse(value, ISO);
    } catch (Exception ignored) {
      return null;
    }
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }
}
