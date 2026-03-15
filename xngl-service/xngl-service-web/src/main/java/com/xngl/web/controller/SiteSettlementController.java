package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.site.SiteSettlement;
import com.xngl.infrastructure.persistence.mapper.SiteSettlementMapper;
import com.xngl.manager.site.SiteService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sites")
public class SiteSettlementController {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final DateTimeFormatter NO_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final SiteService siteService;
  private final SiteSettlementMapper siteSettlementMapper;

  public SiteSettlementController(
      SiteService siteService, SiteSettlementMapper siteSettlementMapper) {
    this.siteService = siteService;
    this.siteSettlementMapper = siteSettlementMapper;
  }

  @GetMapping("/{siteId}/settlements")
  public ApiResult<PageResult<SiteSettlementListItemDto>> list(
      @PathVariable Long siteId,
      @RequestParam(required = false) String settlementStatus,
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize) {
    Site site = siteService.getById(siteId);
    if (site == null) {
      return ApiResult.fail(404, "场地不存在");
    }

    LambdaQueryWrapper<SiteSettlement> query = new LambdaQueryWrapper<>();
    query.eq(SiteSettlement::getSiteId, siteId);
    if (StringUtils.hasText(settlementStatus)) {
      query.eq(SiteSettlement::getSettlementStatus, settlementStatus);
    }
    if (startDate != null) {
      query.ge(SiteSettlement::getSettlementDate, startDate);
    }
    if (endDate != null) {
      query.le(SiteSettlement::getSettlementDate, endDate);
    }
    query.orderByDesc(SiteSettlement::getSettlementDate).orderByDesc(SiteSettlement::getId);

    IPage<SiteSettlement> page =
        siteSettlementMapper.selectPage(new Page<>(pageNo, pageSize), query);
    List<SiteSettlementListItemDto> records =
        page.getRecords().stream().map(this::toListItem).collect(Collectors.toList());
    return ApiResult.ok(
        new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/settlements/{id}")
  public ApiResult<SiteSettlementDetailDto> get(@PathVariable Long id) {
    SiteSettlement settlement = siteSettlementMapper.selectById(id);
    if (settlement == null) {
      return ApiResult.fail(404, "场地结算单不存在");
    }
    return ApiResult.ok(toDetail(settlement));
  }

  @PostMapping("/{siteId}/settlements")
  public ApiResult<String> create(
      @PathVariable Long siteId, @RequestBody SiteSettlementCreateRequest request) {
    Site site = siteService.getById(siteId);
    if (site == null) {
      return ApiResult.fail(404, "场地不存在");
    }
    if (request.getPeriodStart() == null || request.getPeriodEnd() == null) {
      return ApiResult.fail(400, "结算周期必填");
    }
    if (request.getSettlementDate() == null) {
      return ApiResult.fail(400, "结算日期必填");
    }
    if (request.getPeriodStart().isAfter(request.getPeriodEnd())) {
      return ApiResult.fail(400, "结算开始日期不能晚于结束日期");
    }
    if (request.getTotalVolume() == null
        || request.getUnitPrice() == null
        || request.getTotalVolume().compareTo(BigDecimal.ZERO) < 0
        || request.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
      return ApiResult.fail(400, "消纳量和单价必须大于等于 0");
    }

    SiteSettlement settlement = new SiteSettlement();
    settlement.setSiteId(siteId);
    settlement.setSettlementNo(generateSettlementNo(siteId));
    settlement.setPeriodStart(request.getPeriodStart());
    settlement.setPeriodEnd(request.getPeriodEnd());
    settlement.setSettlementDate(request.getSettlementDate());
    settlement.setTotalVolume(request.getTotalVolume());
    settlement.setUnitPrice(request.getUnitPrice());

    // 结算金额统一由后端计算，避免前端传入和真实口径不一致。
    BigDecimal totalAmount = request.getTotalVolume().multiply(request.getUnitPrice());
    BigDecimal adjustAmount =
        request.getAdjustAmount() == null ? BigDecimal.ZERO : request.getAdjustAmount();
    settlement.setTotalAmount(totalAmount);
    settlement.setAdjustAmount(adjustAmount);
    settlement.setPayableAmount(totalAmount.add(adjustAmount));
    settlement.setSettlementStatus(
        StringUtils.hasText(request.getSettlementStatus())
            ? request.getSettlementStatus().trim()
            : "DRAFT");
    settlement.setApprovalStatus(
        StringUtils.hasText(request.getApprovalStatus())
            ? request.getApprovalStatus().trim()
            : "NOT_SUBMITTED");
    settlement.setRemark(request.getRemark());
    siteSettlementMapper.insert(settlement);
    return ApiResult.ok(String.valueOf(settlement.getId()));
  }

  private String generateSettlementNo(Long siteId) {
    return "SS-" + siteId + "-" + LocalDateTime.now().format(NO_FORMATTER);
  }

  private SiteSettlementListItemDto toListItem(SiteSettlement settlement) {
    return new SiteSettlementListItemDto(
        String.valueOf(settlement.getId()),
        settlement.getSettlementNo(),
        settlement.getPeriodStart(),
        settlement.getPeriodEnd(),
        settlement.getSettlementDate(),
        settlement.getTotalVolume(),
        settlement.getPayableAmount(),
        settlement.getSettlementStatus(),
        settlement.getApprovalStatus());
  }

  private SiteSettlementDetailDto toDetail(SiteSettlement settlement) {
    return new SiteSettlementDetailDto(
        String.valueOf(settlement.getId()),
        settlement.getSiteId() != null ? String.valueOf(settlement.getSiteId()) : null,
        settlement.getSettlementNo(),
        settlement.getPeriodStart(),
        settlement.getPeriodEnd(),
        settlement.getSettlementDate(),
        settlement.getTotalVolume(),
        settlement.getUnitPrice(),
        settlement.getTotalAmount(),
        settlement.getAdjustAmount(),
        settlement.getPayableAmount(),
        settlement.getSettlementStatus(),
        settlement.getApprovalStatus(),
        settlement.getRemark(),
        settlement.getCreateTime() != null ? settlement.getCreateTime().format(ISO) : null,
        settlement.getUpdateTime() != null ? settlement.getUpdateTime().format(ISO) : null);
  }

  @Data
  public static class SiteSettlementCreateRequest {
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate settlementDate;
    private BigDecimal totalVolume;
    private BigDecimal unitPrice;
    private BigDecimal adjustAmount;
    private String settlementStatus;
    private String approvalStatus;
    private String remark;
  }

  @Data
  public static class SiteSettlementListItemDto {
    private final String id;
    private final String settlementNo;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final LocalDate settlementDate;
    private final BigDecimal totalVolume;
    private final BigDecimal payableAmount;
    private final String settlementStatus;
    private final String approvalStatus;
  }

  @Data
  public static class SiteSettlementDetailDto {
    private final String id;
    private final String siteId;
    private final String settlementNo;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final LocalDate settlementDate;
    private final BigDecimal totalVolume;
    private final BigDecimal unitPrice;
    private final BigDecimal totalAmount;
    private final BigDecimal adjustAmount;
    private final BigDecimal payableAmount;
    private final String settlementStatus;
    private final String approvalStatus;
    private final String remark;
    private final String createTime;
    private final String updateTime;
  }
}
