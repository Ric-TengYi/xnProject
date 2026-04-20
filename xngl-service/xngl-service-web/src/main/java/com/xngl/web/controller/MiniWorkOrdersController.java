package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractTicket;
import com.xngl.infrastructure.persistence.entity.event.ManualEvent;
import com.xngl.infrastructure.persistence.entity.event.ManualEventAuditLog;
import com.xngl.infrastructure.persistence.entity.miniprogram.MiniCheckinExceptionApply;
import com.xngl.infrastructure.persistence.entity.miniprogram.MiniDelayApply;
import com.xngl.infrastructure.persistence.entity.miniprogram.MiniExcavationPhoto;
import com.xngl.infrastructure.persistence.entity.miniprogram.MiniFeedback;
import com.xngl.infrastructure.persistence.entity.miniprogram.MiniManualDisposalRecord;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.project.ProjectConfig;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.site.SiteOperationConfig;
import com.xngl.infrastructure.persistence.entity.site.SitePersonnelConfig;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ContractTicketMapper;
import com.xngl.infrastructure.persistence.mapper.ManualEventAuditLogMapper;
import com.xngl.infrastructure.persistence.mapper.ManualEventMapper;
import com.xngl.infrastructure.persistence.mapper.MiniCheckinExceptionApplyMapper;
import com.xngl.infrastructure.persistence.mapper.MiniDelayApplyMapper;
import com.xngl.infrastructure.persistence.mapper.MiniExcavationPhotoMapper;
import com.xngl.infrastructure.persistence.mapper.MiniFeedbackMapper;
import com.xngl.infrastructure.persistence.mapper.MiniManualDisposalRecordMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectConfigMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.infrastructure.persistence.mapper.SiteOperationConfigMapper;
import com.xngl.infrastructure.persistence.mapper.SitePersonnelConfigMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.mini.MiniCheckinExceptionCreateDto;
import com.xngl.web.dto.mini.MiniCheckinExceptionDto;
import com.xngl.web.dto.mini.MiniDelayApplyCreateDto;
import com.xngl.web.dto.mini.MiniDelayApplyDto;
import com.xngl.web.dto.mini.MiniExcavationPhotoCreateDto;
import com.xngl.web.dto.mini.MiniExcavationPhotoDto;
import com.xngl.web.dto.mini.MiniFeedbackCloseDto;
import com.xngl.web.dto.mini.MiniFeedbackCreateDto;
import com.xngl.web.dto.mini.MiniFeedbackDto;
import com.xngl.web.dto.mini.MiniManualDisposalCreateDto;
import com.xngl.web.dto.mini.MiniManualDisposalDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mini")
public class MiniWorkOrdersController {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final DateTimeFormatter EVENT_NO = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  private static final Pattern PLATE_PATTERN =
      Pattern.compile("([\\u4e00-\\u9fa5][A-Z][A-Z0-9]{5,6})", Pattern.CASE_INSENSITIVE);

  private final MiniExcavationPhotoMapper miniExcavationPhotoMapper;
  private final MiniCheckinExceptionApplyMapper miniCheckinExceptionApplyMapper;
  private final MiniDelayApplyMapper miniDelayApplyMapper;
  private final MiniFeedbackMapper miniFeedbackMapper;
  private final MiniManualDisposalRecordMapper miniManualDisposalRecordMapper;
  private final ManualEventMapper manualEventMapper;
  private final ManualEventAuditLogMapper manualEventAuditLogMapper;
  private final ContractTicketMapper contractTicketMapper;
  private final ContractMapper contractMapper;
  private final ProjectMapper projectMapper;
  private final ProjectConfigMapper projectConfigMapper;
  private final SiteMapper siteMapper;
  private final SiteOperationConfigMapper siteOperationConfigMapper;
  private final SitePersonnelConfigMapper sitePersonnelConfigMapper;
  private final VehicleMapper vehicleMapper;
  private final UserContext userContext;

  public MiniWorkOrdersController(
      MiniExcavationPhotoMapper miniExcavationPhotoMapper,
      MiniCheckinExceptionApplyMapper miniCheckinExceptionApplyMapper,
      MiniDelayApplyMapper miniDelayApplyMapper,
      MiniFeedbackMapper miniFeedbackMapper,
      MiniManualDisposalRecordMapper miniManualDisposalRecordMapper,
      ManualEventMapper manualEventMapper,
      ManualEventAuditLogMapper manualEventAuditLogMapper,
      ContractTicketMapper contractTicketMapper,
      ContractMapper contractMapper,
      ProjectMapper projectMapper,
      ProjectConfigMapper projectConfigMapper,
      SiteMapper siteMapper,
      SiteOperationConfigMapper siteOperationConfigMapper,
      SitePersonnelConfigMapper sitePersonnelConfigMapper,
      VehicleMapper vehicleMapper,
      UserContext userContext) {
    this.miniExcavationPhotoMapper = miniExcavationPhotoMapper;
    this.miniCheckinExceptionApplyMapper = miniCheckinExceptionApplyMapper;
    this.miniDelayApplyMapper = miniDelayApplyMapper;
    this.miniFeedbackMapper = miniFeedbackMapper;
    this.miniManualDisposalRecordMapper = miniManualDisposalRecordMapper;
    this.manualEventMapper = manualEventMapper;
    this.manualEventAuditLogMapper = manualEventAuditLogMapper;
    this.contractTicketMapper = contractTicketMapper;
    this.contractMapper = contractMapper;
    this.projectMapper = projectMapper;
    this.projectConfigMapper = projectConfigMapper;
    this.siteMapper = siteMapper;
    this.siteOperationConfigMapper = siteOperationConfigMapper;
    this.sitePersonnelConfigMapper = sitePersonnelConfigMapper;
    this.vehicleMapper = vehicleMapper;
    this.userContext = userContext;
  }

  @PostMapping("/photos")
  public ApiResult<MiniExcavationPhotoDto> createPhoto(
      @RequestBody MiniExcavationPhotoCreateDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validatePhoto(body);
    Project project = requireProject(body.getProjectId());
    Site site = body.getSiteId() != null ? requireAccessibleSite(currentUser, body.getSiteId()) : null;
    validateProjectCheckinConfig(currentUser.getTenantId(), project.getId());

    PlateRecognition recognition = recognizePlate(body.getPlateNo(), body.getFileUrl(), body.getRemark());

    MiniExcavationPhoto photo = new MiniExcavationPhoto();
    photo.setTenantId(currentUser.getTenantId());
    photo.setProjectId(project.getId());
    photo.setSiteId(site != null ? site.getId() : null);
    photo.setUserId(currentUser.getId());
    photo.setReporterName(resolveUserDisplayName(currentUser));
    photo.setPlateNo(recognition.plateNo());
    photo.setRecognitionSource(recognition.source());
    photo.setPhotoType(normalizeText(body.getPhotoType(), "EXCAVATION"));
    photo.setFileUrl(body.getFileUrl().trim());
    photo.setLongitude(body.getLongitude());
    photo.setLatitude(body.getLatitude());
    photo.setShootTime(parseDateTime(body.getShootTime(), LocalDateTime.now()));
    photo.setRemark(trimToNull(body.getRemark()));
    photo.setAuditStatus("SUBMITTED");
    miniExcavationPhotoMapper.insert(photo);
    return ApiResult.ok(toPhotoDto(photo, project, site));
  }

  @GetMapping("/photos")
  public ApiResult<List<MiniExcavationPhotoDto>> listPhotos(
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) Long siteId,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validateSiteAccessIfPresent(currentUser, siteId);
    List<MiniExcavationPhoto> rows =
        miniExcavationPhotoMapper.selectList(
            new LambdaQueryWrapper<MiniExcavationPhoto>()
                .eq(MiniExcavationPhoto::getTenantId, currentUser.getTenantId())
                .eq(MiniExcavationPhoto::getUserId, currentUser.getId())
                .eq(projectId != null, MiniExcavationPhoto::getProjectId, projectId)
                .eq(siteId != null, MiniExcavationPhoto::getSiteId, siteId)
                .orderByDesc(MiniExcavationPhoto::getShootTime)
                .orderByDesc(MiniExcavationPhoto::getId));
    return ApiResult.ok(toPhotoDtos(rows));
  }

  @PostMapping("/checkin-exceptions")
  public ApiResult<MiniCheckinExceptionDto> createCheckinException(
      @RequestBody MiniCheckinExceptionCreateDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    if (body == null || body.getCheckinId() == null) {
      throw new BizException(400, "请选择打卡记录");
    }
    if (!StringUtils.hasText(body.getExceptionType()) || !StringUtils.hasText(body.getReason())) {
      throw new BizException(400, "异常类型和原因不能为空");
    }
    ContractTicket ticket = requireTicket(body.getCheckinId(), currentUser.getTenantId());
    Contract contract = requireContract(ticket.getContractId(), currentUser.getTenantId());
    validateSiteAccessIfPresent(currentUser, contract.getSiteId());

    MiniCheckinExceptionApply apply = new MiniCheckinExceptionApply();
    apply.setTenantId(currentUser.getTenantId());
    apply.setCheckinId(ticket.getId());
    apply.setProjectId(contract.getProjectId());
    apply.setSiteId(contract.getSiteId());
    apply.setUserId(currentUser.getId());
    apply.setReporterName(resolveUserDisplayName(currentUser));
    apply.setExceptionType(body.getExceptionType().trim().toUpperCase());
    apply.setReason(body.getReason().trim());
    apply.setAttachmentUrls(trimToNull(body.getAttachmentUrls()));
    apply.setStatus("SUBMITTED");
    apply.setSourceChannel("MINI");
    miniCheckinExceptionApplyMapper.insert(apply);

    ManualEvent event =
        createManualEvent(
            currentUser,
            "CHECKIN_EXCEPTION",
            "打卡异常申报",
            "打卡记录 " + firstNonBlank(ticket.getTicketNo(), String.valueOf(ticket.getId())) + " 异常类型：" + apply.getExceptionType() + "；原因：" + apply.getReason(),
            contract.getProjectId(),
            contract.getSiteId(),
            apply.getAttachmentUrls(),
            "HIGH",
            null,
            null);
    apply.setLinkedEventId(event.getId());
    miniCheckinExceptionApplyMapper.updateById(apply);
    return ApiResult.ok(toCheckinExceptionDto(apply));
  }

  @GetMapping("/checkin-exceptions")
  public ApiResult<List<MiniCheckinExceptionDto>> listCheckinExceptions(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<MiniCheckinExceptionApply> rows =
        miniCheckinExceptionApplyMapper.selectList(
            new LambdaQueryWrapper<MiniCheckinExceptionApply>()
                .eq(MiniCheckinExceptionApply::getTenantId, currentUser.getTenantId())
                .eq(MiniCheckinExceptionApply::getUserId, currentUser.getId())
                .orderByDesc(MiniCheckinExceptionApply::getId));
    return ApiResult.ok(rows.stream().map(this::toCheckinExceptionDto).toList());
  }

  @PostMapping("/delay-applies")
  public ApiResult<MiniDelayApplyDto> createDelayApply(
      @RequestBody MiniDelayApplyCreateDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    if (body == null || !StringUtils.hasText(body.getReason()) || !StringUtils.hasText(body.getRequestedEndTime())) {
      throw new BizException(400, "延期原因和截止时间不能为空");
    }
    validateSiteAccessIfPresent(currentUser, body.getSiteId());
    if (body.getProjectId() != null) {
      requireProject(body.getProjectId());
    }

    MiniDelayApply apply = new MiniDelayApply();
    apply.setTenantId(currentUser.getTenantId());
    apply.setBizType(normalizeText(body.getBizType(), "PROJECT"));
    apply.setBizId(body.getBizId());
    apply.setProjectId(body.getProjectId());
    apply.setSiteId(body.getSiteId());
    apply.setUserId(currentUser.getId());
    apply.setReporterName(resolveUserDisplayName(currentUser));
    apply.setRequestedEndTime(parseDateTime(body.getRequestedEndTime(), null));
    apply.setReason(body.getReason().trim());
    apply.setAttachmentUrls(trimToNull(body.getAttachmentUrls()));
    apply.setStatus("SUBMITTED");
    apply.setSourceChannel("MINI");
    miniDelayApplyMapper.insert(apply);

    ManualEvent event =
        createManualEvent(
            currentUser,
            "DELAY",
            "延期申报",
            "业务类型：" + apply.getBizType() + "；延期至：" + apply.getRequestedEndTime().format(ISO) + "；原因：" + apply.getReason(),
            apply.getProjectId(),
            apply.getSiteId(),
            apply.getAttachmentUrls(),
            "MEDIUM",
            apply.getRequestedEndTime(),
            null);
    apply.setLinkedEventId(event.getId());
    miniDelayApplyMapper.updateById(apply);
    return ApiResult.ok(toDelayApplyDto(apply));
  }

  @GetMapping("/delay-applies")
  public ApiResult<List<MiniDelayApplyDto>> listDelayApplies(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<MiniDelayApply> rows =
        miniDelayApplyMapper.selectList(
            new LambdaQueryWrapper<MiniDelayApply>()
                .eq(MiniDelayApply::getTenantId, currentUser.getTenantId())
                .eq(MiniDelayApply::getUserId, currentUser.getId())
                .orderByDesc(MiniDelayApply::getRequestedEndTime)
                .orderByDesc(MiniDelayApply::getId));
    return ApiResult.ok(rows.stream().map(this::toDelayApplyDto).toList());
  }

  @PostMapping("/feedbacks")
  public ApiResult<MiniFeedbackDto> createFeedback(
      @RequestBody MiniFeedbackCreateDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    if (body == null || !StringUtils.hasText(body.getFeedbackType()) || !StringUtils.hasText(body.getTitle()) || !StringUtils.hasText(body.getContent())) {
      throw new BizException(400, "反馈类型、标题和内容不能为空");
    }
    validateSiteAccessIfPresent(currentUser, body.getSiteId());
    if (body.getProjectId() != null) {
      requireProject(body.getProjectId());
    }

    MiniFeedback feedback = new MiniFeedback();
    feedback.setTenantId(currentUser.getTenantId());
    feedback.setFeedbackType(body.getFeedbackType().trim().toUpperCase());
    feedback.setProjectId(body.getProjectId());
    feedback.setSiteId(body.getSiteId());
    feedback.setUserId(currentUser.getId());
    feedback.setReporterName(resolveUserDisplayName(currentUser));
    feedback.setTitle(body.getTitle().trim());
    feedback.setContent(body.getContent().trim());
    feedback.setAttachmentUrls(trimToNull(body.getAttachmentUrls()));
    feedback.setReportAddress(trimToNull(body.getReportAddress()));
    feedback.setStatus("SUBMITTED");
    feedback.setSourceChannel("MINI");
    miniFeedbackMapper.insert(feedback);

    ManualEvent event =
        createManualEvent(
            currentUser,
            "FEEDBACK",
            feedback.getTitle(),
            feedback.getContent(),
            feedback.getProjectId(),
            feedback.getSiteId(),
            feedback.getAttachmentUrls(),
            "MEDIUM",
            null,
            feedback.getReportAddress());
    feedback.setLinkedEventId(event.getId());
    miniFeedbackMapper.updateById(feedback);
    return ApiResult.ok(toFeedbackDto(feedback));
  }

  @GetMapping("/feedbacks")
  public ApiResult<List<MiniFeedbackDto>> listFeedbacks(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<MiniFeedback> rows =
        miniFeedbackMapper.selectList(
            new LambdaQueryWrapper<MiniFeedback>()
                .eq(MiniFeedback::getTenantId, currentUser.getTenantId())
                .eq(MiniFeedback::getUserId, currentUser.getId())
                .orderByDesc(MiniFeedback::getId));
    return ApiResult.ok(rows.stream().map(this::toFeedbackDto).toList());
  }

  @PutMapping("/feedbacks/{id}/close")
  public ApiResult<MiniFeedbackDto> closeFeedback(
      @PathVariable Long id, @RequestBody(required = false) MiniFeedbackCloseDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    MiniFeedback feedback = miniFeedbackMapper.selectById(id);
    if (feedback == null || !Objects.equals(feedback.getTenantId(), currentUser.getTenantId()) || !Objects.equals(feedback.getUserId(), currentUser.getId())) {
      throw new BizException(404, "反馈不存在");
    }
    feedback.setStatus("CLOSED");
    feedback.setCloseTime(LocalDateTime.now());
    feedback.setCloseRemark(trimToNull(body != null ? body.getCloseRemark() : null));
    miniFeedbackMapper.updateById(feedback);

    if (feedback.getLinkedEventId() != null) {
      ManualEvent event = manualEventMapper.selectById(feedback.getLinkedEventId());
      if (event != null) {
        event.setStatus("CLOSED");
        event.setCurrentAuditNode("DONE");
        event.setCloseTime(feedback.getCloseTime());
        event.setCloseRemark(feedback.getCloseRemark());
        manualEventMapper.updateById(event);
        insertAuditLog(currentUser, event, "CLOSE", "CLOSED", feedback.getCloseRemark());
      }
    }
    return ApiResult.ok(toFeedbackDto(feedback));
  }

  @PostMapping("/manual-disposals")
  public ApiResult<MiniManualDisposalDto> createManualDisposal(
      @RequestBody MiniManualDisposalCreateDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    if (body == null || body.getSiteId() == null || body.getContractId() == null || body.getVolume() == null) {
      throw new BizException(400, "场地、合同和消纳方量不能为空");
    }
    if (body.getVolume().compareTo(BigDecimal.ZERO) <= 0) {
      throw new BizException(400, "消纳方量必须大于 0");
    }
    Site site = requireAccessibleSite(currentUser, body.getSiteId());
    ensureManualDisposalEnabled(currentUser.getTenantId(), site.getId());
    Contract contract = requireContract(body.getContractId(), currentUser.getTenantId());
    if (!Objects.equals(contract.getSiteId(), site.getId())) {
      throw new BizException(400, "手动消纳合同与场地不匹配");
    }
    Vehicle vehicle = body.getVehicleId() != null ? requireVehicle(body.getVehicleId(), currentUser.getTenantId()) : null;
    Project project = contract.getProjectId() != null ? requireProject(contract.getProjectId()) : null;

    LocalDateTime disposalTime = parseDateTime(body.getDisposalTime(), LocalDateTime.now());
    BigDecimal amount =
        body.getAmount() != null
            ? body.getAmount()
            : defaultDecimal(contract.getUnitPrice()).multiply(body.getVolume()).setScale(2, RoundingMode.HALF_UP);
    String plateNo =
        StringUtils.hasText(body.getPlateNo())
            ? body.getPlateNo().trim().toUpperCase()
            : (vehicle != null ? vehicle.getPlateNo() : null);

    ContractTicket ticket = new ContractTicket();
    ticket.setTenantId(currentUser.getTenantId());
    ticket.setContractId(contract.getId());
    ticket.setTicketNo(generateManualTicketNo());
    ticket.setTicketType("MANUAL");
    ticket.setTicketDate(disposalTime.toLocalDate());
    ticket.setAmount(amount);
    ticket.setVolume(body.getVolume());
    ticket.setStatus("CONFIRMED");
    ticket.setRemark("mini manual disposal");
    ticket.setCreatorId(currentUser.getId());
    contractTicketMapper.insert(ticket);

    MiniManualDisposalRecord record = new MiniManualDisposalRecord();
    record.setTenantId(currentUser.getTenantId());
    record.setSiteId(site.getId());
    record.setContractId(contract.getId());
    record.setProjectId(contract.getProjectId());
    record.setVehicleId(vehicle != null ? vehicle.getId() : null);
    record.setUserId(currentUser.getId());
    record.setReporterName(resolveUserDisplayName(currentUser));
    record.setPlateNo(trimToNull(plateNo));
    record.setDisposalTime(disposalTime);
    record.setVolume(body.getVolume());
    record.setAmount(amount);
    record.setWeightTons(body.getWeightTons());
    record.setPhotoUrls(trimToNull(body.getPhotoUrls()));
    record.setRemark(trimToNull(body.getRemark()));
    record.setStatus("CONFIRMED");
    record.setTicketId(ticket.getId());
    record.setSourceChannel("MINI");
    miniManualDisposalRecordMapper.insert(record);
    return ApiResult.ok(toManualDisposalDto(record, site, contract, project, vehicle, ticket));
  }

  @GetMapping("/manual-disposals")
  public ApiResult<List<MiniManualDisposalDto>> listManualDisposals(
      @RequestParam(required = false) Long siteId, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validateSiteAccessIfPresent(currentUser, siteId);
    List<MiniManualDisposalRecord> rows =
        miniManualDisposalRecordMapper.selectList(
            new LambdaQueryWrapper<MiniManualDisposalRecord>()
                .eq(MiniManualDisposalRecord::getTenantId, currentUser.getTenantId())
                .eq(siteId != null, MiniManualDisposalRecord::getSiteId, siteId)
                .orderByDesc(MiniManualDisposalRecord::getDisposalTime)
                .orderByDesc(MiniManualDisposalRecord::getId));
    return ApiResult.ok(toManualDisposalDtos(rows));
  }

  @GetMapping("/manual-disposals/{id}")
  public ApiResult<MiniManualDisposalDto> getManualDisposal(
      @PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    MiniManualDisposalRecord record = miniManualDisposalRecordMapper.selectById(id);
    if (record == null || !Objects.equals(record.getTenantId(), currentUser.getTenantId())) {
      throw new BizException(404, "手动消纳记录不存在");
    }
    validateSiteAccessIfPresent(currentUser, record.getSiteId());
    Site site = siteMapper.selectById(record.getSiteId());
    Contract contract = record.getContractId() != null ? contractMapper.selectById(record.getContractId()) : null;
    Project project = record.getProjectId() != null ? projectMapper.selectById(record.getProjectId()) : null;
    Vehicle vehicle = record.getVehicleId() != null ? vehicleMapper.selectById(record.getVehicleId()) : null;
    ContractTicket ticket = record.getTicketId() != null ? contractTicketMapper.selectById(record.getTicketId()) : null;
    return ApiResult.ok(toManualDisposalDto(record, site, contract, project, vehicle, ticket));
  }

  private void validatePhoto(MiniExcavationPhotoCreateDto body) {
    if (body == null || body.getProjectId() == null || !StringUtils.hasText(body.getFileUrl())) {
      throw new BizException(400, "项目和图片地址不能为空");
    }
  }

  private void validateProjectCheckinConfig(Long tenantId, Long projectId) {
    ProjectConfig config =
        projectConfigMapper.selectOne(
            new LambdaQueryWrapper<ProjectConfig>()
                .eq(ProjectConfig::getTenantId, tenantId)
                .eq(ProjectConfig::getProjectId, projectId)
                .last("limit 1"));
    if (config != null && Objects.equals(config.getCheckinEnabled(), 0)) {
      throw new BizException(400, "当前项目未启用出土打卡");
    }
  }

  private ManualEvent createManualEvent(
      User currentUser,
      String eventType,
      String title,
      String content,
      Long projectId,
      Long siteId,
      String attachmentUrls,
      String priority,
      LocalDateTime deadlineTime,
      String reportAddress) {
    ManualEvent event = new ManualEvent();
    event.setTenantId(currentUser.getTenantId());
    event.setEventNo(nextManualEventNo());
    event.setEventType(eventType);
    event.setTitle(title);
    event.setContent(content);
    event.setSourceChannel("MINI");
    event.setReportAddress(trimToNull(reportAddress));
    event.setProjectId(projectId);
    event.setSiteId(siteId);
    event.setReporterId(currentUser.getId());
    event.setReporterName(resolveUserDisplayName(currentUser));
    event.setContactPhone(currentUser.getMobile());
    event.setPriority(priority);
    event.setStatus("PENDING_AUDIT");
    event.setCurrentAuditNode("MANUAL_EVENT_AUDIT");
    event.setOccurTime(LocalDateTime.now());
    event.setDeadlineTime(deadlineTime);
    event.setReportTime(LocalDateTime.now());
    event.setAttachmentUrls(trimToNull(attachmentUrls));
    manualEventMapper.insert(event);
    insertAuditLog(currentUser, event, "SUBMIT", "PENDING_AUDIT", "小程序提交");
    return event;
  }

  private String nextManualEventNo() {
    return "ME-"
        + LocalDateTime.now().format(EVENT_NO)
        + "-"
        + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
  }

  private void insertAuditLog(User currentUser, ManualEvent event, String action, String resultStatus, String comment) {
    ManualEventAuditLog log = new ManualEventAuditLog();
    log.setTenantId(currentUser.getTenantId());
    log.setEventId(event.getId());
    log.setNodeCode(event.getCurrentAuditNode());
    log.setAction(action);
    log.setResultStatus(resultStatus);
    log.setAuditorId(currentUser.getId());
    log.setAuditorName(resolveUserDisplayName(currentUser));
    log.setComment(trimToNull(comment));
    log.setAuditTime(LocalDateTime.now());
    manualEventAuditLogMapper.insert(log);
  }

  private List<MiniExcavationPhotoDto> toPhotoDtos(List<MiniExcavationPhoto> rows) {
    if (rows.isEmpty()) {
      return List.of();
    }
    Map<Long, Project> projectMap = loadProjects(rows.stream().map(MiniExcavationPhoto::getProjectId).collect(Collectors.toSet()));
    Map<Long, Site> siteMap = loadSites(rows.stream().map(MiniExcavationPhoto::getSiteId).collect(Collectors.toSet()));
    return rows.stream()
        .map(row -> toPhotoDto(row, projectMap.get(row.getProjectId()), siteMap.get(row.getSiteId())))
        .toList();
  }

  private MiniExcavationPhotoDto toPhotoDto(MiniExcavationPhoto row, Project project, Site site) {
    return new MiniExcavationPhotoDto(
        stringId(row.getId()),
        stringId(row.getProjectId()),
        project != null ? project.getName() : null,
        stringId(row.getSiteId()),
        site != null ? site.getName() : null,
        row.getPlateNo(),
        row.getRecognitionSource(),
        row.getFileUrl(),
        row.getPhotoType(),
        row.getLongitude(),
        row.getLatitude(),
        row.getShootTime() != null ? row.getShootTime().format(ISO) : null,
        row.getRemark(),
        row.getAuditStatus());
  }

  private MiniCheckinExceptionDto toCheckinExceptionDto(MiniCheckinExceptionApply row) {
    Project project = row.getProjectId() != null ? projectMapper.selectById(row.getProjectId()) : null;
    Site site = row.getSiteId() != null ? siteMapper.selectById(row.getSiteId()) : null;
    return new MiniCheckinExceptionDto(
        stringId(row.getId()),
        stringId(row.getCheckinId()),
        stringId(row.getProjectId()),
        project != null ? project.getName() : null,
        stringId(row.getSiteId()),
        site != null ? site.getName() : null,
        row.getExceptionType(),
        row.getReason(),
        row.getAttachmentUrls(),
        row.getStatus(),
        stringId(row.getLinkedEventId()),
        row.getCreateTime() != null ? row.getCreateTime().format(ISO) : null);
  }

  private MiniDelayApplyDto toDelayApplyDto(MiniDelayApply row) {
    Project project = row.getProjectId() != null ? projectMapper.selectById(row.getProjectId()) : null;
    Site site = row.getSiteId() != null ? siteMapper.selectById(row.getSiteId()) : null;
    return new MiniDelayApplyDto(
        stringId(row.getId()),
        row.getBizType(),
        stringId(row.getBizId()),
        stringId(row.getProjectId()),
        project != null ? project.getName() : null,
        stringId(row.getSiteId()),
        site != null ? site.getName() : null,
        row.getRequestedEndTime() != null ? row.getRequestedEndTime().format(ISO) : null,
        row.getReason(),
        row.getAttachmentUrls(),
        row.getStatus(),
        stringId(row.getLinkedEventId()),
        row.getCreateTime() != null ? row.getCreateTime().format(ISO) : null);
  }

  private MiniFeedbackDto toFeedbackDto(MiniFeedback row) {
    Project project = row.getProjectId() != null ? projectMapper.selectById(row.getProjectId()) : null;
    Site site = row.getSiteId() != null ? siteMapper.selectById(row.getSiteId()) : null;
    return new MiniFeedbackDto(
        stringId(row.getId()),
        row.getFeedbackType(),
        stringId(row.getProjectId()),
        project != null ? project.getName() : null,
        stringId(row.getSiteId()),
        site != null ? site.getName() : null,
        row.getTitle(),
        row.getContent(),
        row.getAttachmentUrls(),
        row.getReportAddress(),
        row.getStatus(),
        row.getHandlerName(),
        row.getCloseTime() != null ? row.getCloseTime().format(ISO) : null,
        row.getCloseRemark(),
        stringId(row.getLinkedEventId()),
        row.getCreateTime() != null ? row.getCreateTime().format(ISO) : null);
  }

  private Map<Long, Project> loadProjects(Set<Long> ids) {
    Set<Long> validIds = ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    if (validIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return projectMapper.selectBatchIds(validIds).stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(Project::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
  }

  private Map<Long, Site> loadSites(Set<Long> ids) {
    Set<Long> validIds = ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    if (validIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return siteMapper.selectBatchIds(validIds).stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(Site::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
  }

  private ContractTicket requireTicket(Long id, Long tenantId) {
    ContractTicket ticket = contractTicketMapper.selectById(id);
    if (ticket == null || !Objects.equals(ticket.getTenantId(), tenantId)) {
      throw new BizException(404, "打卡记录不存在");
    }
    return ticket;
  }

  private Contract requireContract(Long id, Long tenantId) {
    Contract contract = contractMapper.selectById(id);
    if (contract == null || !Objects.equals(contract.getTenantId(), tenantId)) {
      throw new BizException(404, "关联合同不存在");
    }
    return contract;
  }

  private Vehicle requireVehicle(Long id, Long tenantId) {
    Vehicle vehicle = vehicleMapper.selectById(id);
    if (vehicle == null || !Objects.equals(vehicle.getTenantId(), tenantId)) {
      throw new BizException(404, "车辆不存在");
    }
    return vehicle;
  }

  private Project requireProject(Long id) {
    Project project = projectMapper.selectById(id);
    if (project == null || Objects.equals(project.getDeleted(), 1)) {
      throw new BizException(404, "项目不存在");
    }
    return project;
  }

  private Site requireAccessibleSite(User user, Long siteId) {
    Site site = siteMapper.selectById(siteId);
    if (site == null || Objects.equals(site.getDeleted(), 1)) {
      throw new BizException(404, "场地不存在");
    }
    validateSiteAccessIfPresent(user, siteId);
    return site;
  }

  private void validateSiteAccessIfPresent(User user, Long siteId) {
    if (siteId == null || isAdminUser(user)) {
      return;
    }
    boolean accessible =
        sitePersonnelConfigMapper.selectCount(
                new LambdaQueryWrapper<SitePersonnelConfig>()
                    .eq(SitePersonnelConfig::getTenantId, user.getTenantId())
                    .eq(SitePersonnelConfig::getUserId, user.getId())
                    .eq(SitePersonnelConfig::getSiteId, siteId)
                    .eq(SitePersonnelConfig::getAccountEnabled, 1))
            > 0;
    if (!accessible) {
      throw new BizException(403, "当前账号无权操作该场地");
    }
  }

  private boolean isAdminUser(User user) {
    return user != null
        && StringUtils.hasText(user.getUserType())
        && Arrays.asList("TENANT_ADMIN", "SUPER_ADMIN", "ADMIN")
            .contains(user.getUserType().trim().toUpperCase());
  }

  private PlateRecognition recognizePlate(String plateNo, String fileUrl, String remark) {
    if (StringUtils.hasText(plateNo)) {
      return new PlateRecognition(plateNo.trim().toUpperCase(), "MANUAL");
    }
    String combined = decodeText(firstNonBlank(joinText(fileUrl, remark), fileUrl, remark));
    if (StringUtils.hasText(combined)) {
      Matcher matcher = PLATE_PATTERN.matcher(combined.toUpperCase());
      if (matcher.find()) {
        return new PlateRecognition(matcher.group(1), "OCR_MOCK");
      }
    }
    return new PlateRecognition("未识别", "OCR_MOCK");
  }

  private LocalDateTime parseDateTime(String value, LocalDateTime defaultValue) {
    if (!StringUtils.hasText(value)) {
      return defaultValue;
    }
    try {
      return LocalDateTime.parse(value.trim(), ISO);
    } catch (Exception ignored) {
      try {
        return OffsetDateTime.parse(value.trim()).toLocalDateTime();
      } catch (Exception ex) {
        throw new BizException(400, "时间格式错误");
      }
    }
  }

  private void ensureManualDisposalEnabled(Long tenantId, Long siteId) {
    SiteOperationConfig config =
        siteOperationConfigMapper.selectOne(
            new LambdaQueryWrapper<SiteOperationConfig>()
                .eq(SiteOperationConfig::getTenantId, tenantId)
                .eq(SiteOperationConfig::getSiteId, siteId)
                .last("limit 1"));
    if (config == null || !Objects.equals(config.getManualDisposalEnabled(), 1)) {
      throw new BizException(400, "当前场地未开启手动消纳");
    }
  }

  private String generateManualTicketNo() {
    return "MD-"
        + LocalDateTime.now().format(EVENT_NO)
        + "-"
        + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
  }

  private String resolveUserDisplayName(User user) {
    return firstNonBlank(user.getName(), user.getUsername());
  }

  private String normalizeText(String value, String defaultValue) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase() : defaultValue;
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private BigDecimal defaultDecimal(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private List<MiniManualDisposalDto> toManualDisposalDtos(List<MiniManualDisposalRecord> rows) {
    if (rows == null || rows.isEmpty()) {
      return Collections.emptyList();
    }
    LinkedHashSet<Long> siteIds = new LinkedHashSet<>();
    LinkedHashSet<Long> contractIds = new LinkedHashSet<>();
    LinkedHashSet<Long> projectIds = new LinkedHashSet<>();
    LinkedHashSet<Long> vehicleIds = new LinkedHashSet<>();
    LinkedHashSet<Long> ticketIds = new LinkedHashSet<>();
    for (MiniManualDisposalRecord row : rows) {
      if (row.getSiteId() != null) {
        siteIds.add(row.getSiteId());
      }
      if (row.getContractId() != null) {
        contractIds.add(row.getContractId());
      }
      if (row.getProjectId() != null) {
        projectIds.add(row.getProjectId());
      }
      if (row.getVehicleId() != null) {
        vehicleIds.add(row.getVehicleId());
      }
      if (row.getTicketId() != null) {
        ticketIds.add(row.getTicketId());
      }
    }
    Map<Long, Site> siteMap =
        siteIds.isEmpty()
            ? Collections.emptyMap()
            : siteMapper.selectBatchIds(siteIds).stream()
                .collect(Collectors.toMap(Site::getId, item -> item, (left, right) -> left));
    Map<Long, Contract> contractMap =
        contractIds.isEmpty()
            ? Collections.emptyMap()
            : contractMapper.selectBatchIds(contractIds).stream()
                .collect(Collectors.toMap(Contract::getId, item -> item, (left, right) -> left));
    Map<Long, Project> projectMap =
        projectIds.isEmpty()
            ? Collections.emptyMap()
            : projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(Project::getId, item -> item, (left, right) -> left));
    Map<Long, Vehicle> vehicleMap =
        vehicleIds.isEmpty()
            ? Collections.emptyMap()
            : vehicleMapper.selectBatchIds(vehicleIds).stream()
                .collect(Collectors.toMap(Vehicle::getId, item -> item, (left, right) -> left));
    Map<Long, ContractTicket> ticketMap =
        ticketIds.isEmpty()
            ? Collections.emptyMap()
            : contractTicketMapper.selectBatchIds(ticketIds).stream()
                .collect(Collectors.toMap(ContractTicket::getId, item -> item, (left, right) -> left));
    return rows.stream()
        .map(
            row ->
                toManualDisposalDto(
                    row,
                    siteMap.get(row.getSiteId()),
                    contractMap.get(row.getContractId()),
                    projectMap.get(row.getProjectId()),
                    vehicleMap.get(row.getVehicleId()),
                    ticketMap.get(row.getTicketId())))
        .toList();
  }

  private MiniManualDisposalDto toManualDisposalDto(
      MiniManualDisposalRecord record,
      Site site,
      Contract contract,
      Project project,
      Vehicle vehicle,
      ContractTicket ticket) {
    return new MiniManualDisposalDto(
        record.getId() != null ? String.valueOf(record.getId()) : null,
        site != null && site.getId() != null ? String.valueOf(site.getId()) : null,
        site != null ? site.getName() : null,
        contract != null && contract.getId() != null ? String.valueOf(contract.getId()) : null,
        contract != null ? firstNonBlank(contract.getContractNo(), contract.getCode()) : null,
        project != null && project.getId() != null ? String.valueOf(project.getId()) : null,
        project != null ? project.getName() : (contract != null ? contract.getName() : null),
        vehicle != null && vehicle.getId() != null ? String.valueOf(vehicle.getId()) : null,
        firstNonBlank(record.getPlateNo(), vehicle != null ? vehicle.getPlateNo() : null),
        record.getDisposalTime() != null ? record.getDisposalTime().format(ISO) : null,
        record.getVolume() != null ? record.getVolume().stripTrailingZeros().toPlainString() : "0",
        record.getAmount() != null ? record.getAmount().stripTrailingZeros().toPlainString() : "0",
        record.getWeightTons() != null ? record.getWeightTons().stripTrailingZeros().toPlainString() : null,
        record.getPhotoUrls(),
        record.getRemark(),
        record.getStatus(),
        ticket != null && ticket.getId() != null ? String.valueOf(ticket.getId()) : null,
        ticket != null ? ticket.getTicketNo() : null);
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    return null;
  }

  private String stringId(Long id) {
    return id != null ? String.valueOf(id) : null;
  }

  private String joinText(String left, String right) {
    String leftValue = trimToNull(left);
    String rightValue = trimToNull(right);
    if (leftValue == null) {
      return rightValue;
    }
    if (rightValue == null) {
      return leftValue;
    }
    return leftValue + " " + rightValue;
  }

  private String decodeText(String value) {
    if (!StringUtils.hasText(value)) {
      return value;
    }
    try {
      return URLDecoder.decode(value, StandardCharsets.UTF_8);
    } catch (Exception ex) {
      return value;
    }
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }

  private record PlateRecognition(String plateNo, String source) {}
}
