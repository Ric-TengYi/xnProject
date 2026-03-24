package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.site.SiteDocument;
import com.xngl.infrastructure.persistence.mapper.SiteDocumentMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.site.SiteDocumentSummaryDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/site-documents")
public class SiteDocumentsController {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final SiteDocumentMapper siteDocumentMapper;
  private final SiteMapper siteMapper;
  private final UserContext userContext;

  public SiteDocumentsController(
      SiteDocumentMapper siteDocumentMapper, SiteMapper siteMapper, UserContext userContext) {
    this.siteDocumentMapper = siteDocumentMapper;
    this.siteMapper = siteMapper;
    this.userContext = userContext;
  }

  @GetMapping("/summary")
  public ApiResult<List<SiteDocumentSummaryDto>> summary(
      @RequestParam(required = false) Long siteId,
      @RequestParam(required = false) String stageCode,
      @RequestParam(required = false) String approvalType,
      @RequestParam(required = false) String keyword,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<SiteDocument> rows =
        siteDocumentMapper.selectList(
            new LambdaQueryWrapper<SiteDocument>()
                .eq(SiteDocument::getTenantId, currentUser.getTenantId())
                .eq(siteId != null, SiteDocument::getSiteId, siteId)
                .eq(StringUtils.hasText(stageCode), SiteDocument::getStageCode, normalizeCode(stageCode))
                .eq(
                    StringUtils.hasText(approvalType),
                    SiteDocument::getApprovalType,
                    normalizeCode(approvalType))
                .orderByDesc(SiteDocument::getUpdateTime)
                .orderByDesc(SiteDocument::getId));
    Map<Long, Site> siteMap = loadSiteMap(rows);
    LinkedHashMap<String, SummaryAccumulator> grouped = new LinkedHashMap<>();
    for (SiteDocument row : rows) {
      Site site = siteMap.get(row.getSiteId());
      if (!matchesKeyword(row, site, keyword)) {
        continue;
      }
      String key =
          row.getSiteId()
              + "|"
              + valueOrEmpty(row.getStageCode())
              + "|"
              + valueOrEmpty(row.getApprovalType())
              + "|"
              + valueOrEmpty(row.getDocumentType());
      SummaryAccumulator bucket = grouped.computeIfAbsent(key, unused -> new SummaryAccumulator(row, site));
      bucket.count += 1;
      if (row.getUpdateTime() != null
          && (bucket.lastUpdateTime == null || row.getUpdateTime().isAfter(bucket.lastUpdateTime))) {
        bucket.lastUpdateTime = row.getUpdateTime();
        bucket.uploaderName = row.getUploaderName();
      }
    }
    List<SiteDocumentSummaryDto> records =
        grouped.values().stream()
            .sorted(
                Comparator.comparing((SummaryAccumulator item) -> item.lastUpdateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(item -> valueOrEmpty(item.documentType)))
            .map(SummaryAccumulator::toDto)
            .toList();
    return ApiResult.ok(records);
  }

  private Map<Long, Site> loadSiteMap(List<SiteDocument> rows) {
    List<Long> siteIds = rows.stream().map(SiteDocument::getSiteId).filter(Objects::nonNull).distinct().toList();
    if (siteIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, Site> siteMap = new LinkedHashMap<>();
    for (Site item : siteMapper.selectBatchIds(siteIds)) {
      if (item.getId() != null) {
        siteMap.put(item.getId(), item);
      }
    }
    return siteMap;
  }

  private boolean matchesKeyword(SiteDocument row, Site site, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return true;
    }
    String value = keyword.trim();
    return contains(row.getFileName(), value)
        || contains(row.getDocumentType(), value)
        || contains(row.getApprovalType(), value)
        || contains(row.getStageCode(), value)
        || contains(site != null ? site.getName() : null, value);
  }

  private boolean contains(String source, String keyword) {
    return StringUtils.hasText(source) && source.toLowerCase().contains(keyword.toLowerCase());
  }

  private String valueOrEmpty(String value) {
    return value == null ? "" : value;
  }

  private String normalizeCode(String value) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }

  private static class SummaryAccumulator {
    private final String siteId;
    private final String siteName;
    private final String stageCode;
    private final String approvalType;
    private final String documentType;
    private int count;
    private LocalDateTime lastUpdateTime;
    private String uploaderName;

    private SummaryAccumulator(SiteDocument row, Site site) {
      this.siteId = row.getSiteId() != null ? String.valueOf(row.getSiteId()) : null;
      this.siteName = site != null ? site.getName() : null;
      this.stageCode = row.getStageCode();
      this.approvalType = row.getApprovalType();
      this.documentType = row.getDocumentType();
      this.count = 0;
      this.lastUpdateTime = row.getUpdateTime();
      this.uploaderName = row.getUploaderName();
    }

    private SiteDocumentSummaryDto toDto() {
      return new SiteDocumentSummaryDto(
          siteId,
          siteName,
          stageCode,
          approvalType,
          documentType,
          count,
          lastUpdateTime != null ? lastUpdateTime.format(ISO) : null,
          uploaderName);
    }
  }
}
