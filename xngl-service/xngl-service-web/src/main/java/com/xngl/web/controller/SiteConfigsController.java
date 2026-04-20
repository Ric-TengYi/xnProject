package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.site.SiteDevice;
import com.xngl.infrastructure.persistence.entity.site.SiteDocument;
import com.xngl.infrastructure.persistence.entity.site.SiteOperationConfig;
import com.xngl.infrastructure.persistence.entity.site.SitePersonnelConfig;
import com.xngl.infrastructure.persistence.entity.site.SiteSurveyRecord;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.SiteDeviceMapper;
import com.xngl.infrastructure.persistence.mapper.SiteDocumentMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.infrastructure.persistence.mapper.SiteOperationConfigMapper;
import com.xngl.infrastructure.persistence.mapper.SitePersonnelConfigMapper;
import com.xngl.infrastructure.persistence.mapper.SiteSurveyRecordMapper;
import com.xngl.infrastructure.persistence.mapper.UserMapper;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.site.SiteDeviceDto;
import com.xngl.web.dto.site.SiteDeviceUpsertDto;
import com.xngl.web.dto.site.SiteDocumentDto;
import com.xngl.web.dto.site.SiteDocumentUpsertDto;
import com.xngl.web.dto.site.SiteOperationConfigDto;
import com.xngl.web.dto.site.SiteOperationConfigUpsertDto;
import com.xngl.web.dto.site.SitePersonnelCandidateDto;
import com.xngl.web.dto.site.SitePersonnelConfigDto;
import com.xngl.web.dto.site.SitePersonnelUpsertDto;
import com.xngl.web.dto.site.SiteSurveyRecordDto;
import com.xngl.web.dto.site.SiteSurveyUpsertDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sites/{siteId}")
public class SiteConfigsController {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final SiteMapper siteMapper;
  private final SiteDeviceMapper siteDeviceMapper;
  private final SiteDocumentMapper siteDocumentMapper;
  private final SiteOperationConfigMapper siteOperationConfigMapper;
  private final SitePersonnelConfigMapper sitePersonnelConfigMapper;
  private final SiteSurveyRecordMapper siteSurveyRecordMapper;
  private final UserMapper userMapper;
  private final OrgMapper orgMapper;
  private final UserContext userContext;

  public SiteConfigsController(
      SiteMapper siteMapper,
      SiteDeviceMapper siteDeviceMapper,
      SiteDocumentMapper siteDocumentMapper,
      SiteOperationConfigMapper siteOperationConfigMapper,
      SitePersonnelConfigMapper sitePersonnelConfigMapper,
      SiteSurveyRecordMapper siteSurveyRecordMapper,
      UserMapper userMapper,
      OrgMapper orgMapper,
      UserContext userContext) {
    this.siteMapper = siteMapper;
    this.siteDeviceMapper = siteDeviceMapper;
    this.siteDocumentMapper = siteDocumentMapper;
    this.siteOperationConfigMapper = siteOperationConfigMapper;
    this.sitePersonnelConfigMapper = sitePersonnelConfigMapper;
    this.siteSurveyRecordMapper = siteSurveyRecordMapper;
    this.userMapper = userMapper;
    this.orgMapper = orgMapper;
    this.userContext = userContext;
  }

  @PostMapping("/devices")
  public ApiResult<SiteDeviceDto> createDevice(
      @PathVariable Long siteId,
      @RequestBody SiteDeviceUpsertDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Site site = requireSite(siteId);
    validateDevice(body, currentUser.getTenantId(), null);
    SiteDevice device = new SiteDevice();
    device.setTenantId(currentUser.getTenantId());
    device.setSiteId(site.getId());
    applyDevice(device, body);
    siteDeviceMapper.insert(device);
    return ApiResult.ok(toDeviceDto(siteDeviceMapper.selectById(device.getId())));
  }

  @PutMapping("/devices/{deviceId}")
  public ApiResult<SiteDeviceDto> updateDevice(
      @PathVariable Long siteId,
      @PathVariable Long deviceId,
      @RequestBody SiteDeviceUpsertDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    requireSite(siteId);
    SiteDevice device = siteDeviceMapper.selectById(deviceId);
    if (device == null
        || !Objects.equals(device.getTenantId(), currentUser.getTenantId())
        || !Objects.equals(device.getSiteId(), siteId)) {
      throw new BizException(404, "场地设备不存在");
    }
    validateDevice(body, currentUser.getTenantId(), deviceId);
    applyDevice(device, body);
    siteDeviceMapper.updateById(device);
    return ApiResult.ok(toDeviceDto(siteDeviceMapper.selectById(deviceId)));
  }

  @PutMapping("/operation-config")
  public ApiResult<SiteOperationConfigDto> updateOperationConfig(
      @PathVariable Long siteId,
      @RequestBody SiteOperationConfigUpsertDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    requireSite(siteId);
    SiteOperationConfig config =
        siteOperationConfigMapper.selectOne(
            new LambdaQueryWrapper<SiteOperationConfig>()
                .eq(SiteOperationConfig::getTenantId, currentUser.getTenantId())
                .eq(SiteOperationConfig::getSiteId, siteId)
                .last("limit 1"));
    if (config == null) {
      config = new SiteOperationConfig();
      config.setTenantId(currentUser.getTenantId());
      config.setSiteId(siteId);
    }
    config.setQueueEnabled(Boolean.TRUE.equals(body.getQueueEnabled()) ? 1 : 0);
    config.setMaxQueueCount(body.getMaxQueueCount() != null ? body.getMaxQueueCount() : 0);
    config.setManualDisposalEnabled(Boolean.TRUE.equals(body.getManualDisposalEnabled()) ? 1 : 0);
    config.setRangeCheckRadius(body.getRangeCheckRadius() != null ? body.getRangeCheckRadius() : BigDecimal.ZERO);
    config.setDurationLimitMinutes(body.getDurationLimitMinutes() != null ? body.getDurationLimitMinutes() : 0);
    config.setRemark(StringUtils.hasText(body.getRemark()) ? body.getRemark().trim() : null);
    if (config.getId() == null) {
      siteOperationConfigMapper.insert(config);
    } else {
      siteOperationConfigMapper.updateById(config);
    }
    return ApiResult.ok(
        new SiteOperationConfigDto(
            Objects.equals(config.getQueueEnabled(), 1),
            config.getMaxQueueCount(),
            Objects.equals(config.getManualDisposalEnabled(), 1),
            config.getRangeCheckRadius(),
            config.getDurationLimitMinutes(),
            config.getRemark()));
  }

  @GetMapping("/personnel")
  public ApiResult<List<SitePersonnelConfigDto>> listPersonnel(
      @PathVariable Long siteId, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    requireSite(siteId);
    return ApiResult.ok(loadPersonnelDtos(currentUser.getTenantId(), siteId));
  }

  @GetMapping("/documents")
  public ApiResult<List<SiteDocumentDto>> listDocuments(
      @PathVariable Long siteId,
      @org.springframework.web.bind.annotation.RequestParam(required = false) String stageCode,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Site site = requireSite(siteId);
    return ApiResult.ok(loadDocumentDtos(currentUser.getTenantId(), site, stageCode));
  }

  @GetMapping("/surveys")
  public ApiResult<List<SiteSurveyRecordDto>> listSurveys(
      @PathVariable Long siteId, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Site site = requireSite(siteId);
    return ApiResult.ok(loadSurveyDtos(currentUser.getTenantId(), site));
  }

  @GetMapping("/personnel/candidates")
  public ApiResult<List<SitePersonnelCandidateDto>> listPersonnelCandidates(
      @PathVariable Long siteId, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    requireSite(siteId);
    List<User> users =
        userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .eq(User::getTenantId, currentUser.getTenantId())
                .eq(User::getDeleted, 0)
                .orderByAsc(User::getMainOrgId)
                .orderByAsc(User::getId));
    Map<Long, Org> orgMap = loadOrgMap(users.stream().map(User::getMainOrgId).toList());
    List<SitePersonnelCandidateDto> records =
        users.stream()
            .map(
                user ->
                    new SitePersonnelCandidateDto(
                        user.getId() != null ? String.valueOf(user.getId()) : null,
                        user.getUsername(),
                        user.getName(),
                        user.getMobile(),
                        user.getUserType(),
                        user.getMainOrgId() != null ? String.valueOf(user.getMainOrgId()) : null,
                        user.getMainOrgId() != null && orgMap.get(user.getMainOrgId()) != null
                            ? orgMap.get(user.getMainOrgId()).getOrgName()
                            : null,
                        user.getStatus()))
            .toList();
    return ApiResult.ok(records);
  }

  @PostMapping("/personnel")
  public ApiResult<SitePersonnelConfigDto> createPersonnel(
      @PathVariable Long siteId,
      @RequestBody SitePersonnelUpsertDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    requireSite(siteId);
    User targetUser = requireTargetUser(body.getUserId(), currentUser.getTenantId());
    validatePersonnel(body, currentUser.getTenantId(), siteId, null);
    SitePersonnelConfig config = new SitePersonnelConfig();
    config.setTenantId(currentUser.getTenantId());
    config.setSiteId(siteId);
    applyPersonnel(config, body, targetUser);
    sitePersonnelConfigMapper.insert(config);
    return ApiResult.ok(loadPersonnelDto(config.getId(), currentUser.getTenantId()));
  }

  @PostMapping("/documents")
  public ApiResult<SiteDocumentDto> createDocument(
      @PathVariable Long siteId,
      @RequestBody SiteDocumentUpsertDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Site site = requireSite(siteId);
    validateDocument(body);
    SiteDocument entity = new SiteDocument();
    entity.setTenantId(currentUser.getTenantId());
    entity.setSiteId(siteId);
    applyDocument(entity, body, currentUser);
    siteDocumentMapper.insert(entity);
    return ApiResult.ok(loadDocumentDto(entity.getId(), currentUser.getTenantId(), site));
  }

  @PostMapping("/surveys")
  public ApiResult<SiteSurveyRecordDto> createSurvey(
      @PathVariable Long siteId,
      @RequestBody SiteSurveyUpsertDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Site site = requireSite(siteId);
    validateSurvey(body, currentUser.getTenantId(), siteId, null);
    SiteSurveyRecord record = new SiteSurveyRecord();
    record.setTenantId(currentUser.getTenantId());
    record.setSiteId(siteId);
    applySurvey(record, body);
    siteSurveyRecordMapper.insert(record);
    return ApiResult.ok(toSurveyDto(site, siteSurveyRecordMapper.selectById(record.getId())));
  }

  @PutMapping("/personnel/{personnelId}")
  public ApiResult<SitePersonnelConfigDto> updatePersonnel(
      @PathVariable Long siteId,
      @PathVariable Long personnelId,
      @RequestBody SitePersonnelUpsertDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    requireSite(siteId);
    SitePersonnelConfig config = requirePersonnel(personnelId, currentUser.getTenantId(), siteId);
    User targetUser = requireTargetUser(body.getUserId(), currentUser.getTenantId());
    validatePersonnel(body, currentUser.getTenantId(), siteId, personnelId);
    applyPersonnel(config, body, targetUser);
    sitePersonnelConfigMapper.updateById(config);
    return ApiResult.ok(loadPersonnelDto(config.getId(), currentUser.getTenantId()));
  }

  @PutMapping("/documents/{documentId}")
  public ApiResult<SiteDocumentDto> updateDocument(
      @PathVariable Long siteId,
      @PathVariable Long documentId,
      @RequestBody SiteDocumentUpsertDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Site site = requireSite(siteId);
    SiteDocument entity = requireDocument(documentId, currentUser.getTenantId(), siteId);
    validateDocument(body);
    applyDocument(entity, body, currentUser);
    siteDocumentMapper.updateById(entity);
    return ApiResult.ok(loadDocumentDto(entity.getId(), currentUser.getTenantId(), site));
  }

  @PutMapping("/surveys/{surveyId}")
  public ApiResult<SiteSurveyRecordDto> updateSurvey(
      @PathVariable Long siteId,
      @PathVariable Long surveyId,
      @RequestBody SiteSurveyUpsertDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Site site = requireSite(siteId);
    SiteSurveyRecord record = requireSurvey(surveyId, currentUser.getTenantId(), siteId);
    validateSurvey(body, currentUser.getTenantId(), siteId, surveyId);
    applySurvey(record, body);
    siteSurveyRecordMapper.updateById(record);
    return ApiResult.ok(toSurveyDto(site, siteSurveyRecordMapper.selectById(surveyId)));
  }

  @DeleteMapping("/personnel/{personnelId}")
  public ApiResult<Void> deletePersonnel(
      @PathVariable Long siteId,
      @PathVariable Long personnelId,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    requireSite(siteId);
    SitePersonnelConfig config = requirePersonnel(personnelId, currentUser.getTenantId(), siteId);
    sitePersonnelConfigMapper.deleteById(config.getId());
    return ApiResult.ok();
  }

  @DeleteMapping("/documents/{documentId}")
  public ApiResult<Void> deleteDocument(
      @PathVariable Long siteId,
      @PathVariable Long documentId,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    requireSite(siteId);
    SiteDocument entity = requireDocument(documentId, currentUser.getTenantId(), siteId);
    siteDocumentMapper.deleteById(entity.getId());
    return ApiResult.ok();
  }

  @DeleteMapping("/surveys/{surveyId}")
  public ApiResult<Void> deleteSurvey(
      @PathVariable Long siteId,
      @PathVariable Long surveyId,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    requireSite(siteId);
    SiteSurveyRecord record = requireSurvey(surveyId, currentUser.getTenantId(), siteId);
    siteSurveyRecordMapper.deleteById(record.getId());
    return ApiResult.ok();
  }

  private void validateDevice(SiteDeviceUpsertDto body, Long tenantId, Long currentId) {
    if (!StringUtils.hasText(body.getDeviceCode()) || !StringUtils.hasText(body.getDeviceName())) {
      throw new BizException(400, "设备编码和名称不能为空");
    }
    Long existing =
        siteDeviceMapper.selectCount(
            new LambdaQueryWrapper<SiteDevice>()
                .eq(SiteDevice::getTenantId, tenantId)
                .eq(SiteDevice::getDeviceCode, body.getDeviceCode().trim())
                .ne(currentId != null, SiteDevice::getId, currentId));
    if (existing != null && existing > 0) {
      throw new BizException(400, "设备编码已存在");
    }
  }

  private void applyDevice(SiteDevice device, SiteDeviceUpsertDto body) {
    device.setDeviceCode(body.getDeviceCode().trim());
    device.setDeviceName(body.getDeviceName().trim());
    device.setDeviceType(StringUtils.hasText(body.getDeviceType()) ? body.getDeviceType().trim() : null);
    device.setProvider(StringUtils.hasText(body.getProvider()) ? body.getProvider().trim() : null);
    device.setIpAddress(StringUtils.hasText(body.getIpAddress()) ? body.getIpAddress().trim() : null);
    device.setStatus(StringUtils.hasText(body.getStatus()) ? body.getStatus().trim().toUpperCase() : "ONLINE");
    device.setLng(body.getLng());
    device.setLat(body.getLat());
    device.setRemark(StringUtils.hasText(body.getRemark()) ? body.getRemark().trim() : null);
  }

  private void validateDocument(SiteDocumentUpsertDto body) {
    if (!StringUtils.hasText(body.getStageCode())
        || !StringUtils.hasText(body.getDocumentType())
        || !StringUtils.hasText(body.getFileName())
        || !StringUtils.hasText(body.getFileUrl())) {
      throw new BizException(400, "资料阶段、资料类型、文件名、文件地址不能为空");
    }
    String stageCode = normalizeCode(body.getStageCode());
    if (!isSupportedDocumentStage(stageCode)) {
      throw new BizException(400, "资料阶段不支持，需为审批、建设、运营或移交阶段");
    }
    String expectedStage = resolveDocumentStage(body.getDocumentType());
    if (expectedStage != null && !expectedStage.equals(stageCode)) {
      throw new BizException(400, "资料类型与资料阶段不匹配，应归类到对应生命周期阶段");
    }
    String requiredFormat = resolveFormatRequirement(body.getDocumentType());
    String fileName = body.getFileName().trim();
    String extension = extractExtension(fileName);
    if (!matchesFormat(extension, requiredFormat)) {
      throw new BizException(400, "文件格式不符合要求，应为: " + requiredFormat);
    }
  }

  private void validateSurvey(
      SiteSurveyUpsertDto body, Long tenantId, Long siteId, Long currentId) {
    if (body == null) {
      throw new BizException(400, "请求体不能为空");
    }
    if (!StringUtils.hasText(body.getSurveyNo())) {
      throw new BizException(400, "测绘编号不能为空");
    }
    if (!StringUtils.hasText(body.getSurveyDate())) {
      throw new BizException(400, "测绘日期不能为空");
    }
    LocalDate.parse(body.getSurveyDate());
    Long existing =
        siteSurveyRecordMapper.selectCount(
            new LambdaQueryWrapper<SiteSurveyRecord>()
                .eq(SiteSurveyRecord::getTenantId, tenantId)
                .eq(SiteSurveyRecord::getSiteId, siteId)
                .eq(SiteSurveyRecord::getSurveyNo, body.getSurveyNo().trim())
                .ne(currentId != null, SiteSurveyRecord::getId, currentId));
    if (existing != null && existing > 0) {
      throw new BizException(400, "测绘编号已存在");
    }
  }

  private void applyDocument(SiteDocument entity, SiteDocumentUpsertDto body, User currentUser) {
    entity.setStageCode(normalizeCode(body.getStageCode()));
    entity.setApprovalType(resolveApprovalType(body.getApprovalType(), body.getDocumentType()));
    entity.setDocumentType(normalizeCode(body.getDocumentType()));
    entity.setFileName(body.getFileName().trim());
    entity.setFileUrl(body.getFileUrl().trim());
    entity.setFileSize(body.getFileSize());
    entity.setMimeType(trimToNull(body.getMimeType()));
    entity.setFormatRequirement(resolveFormatRequirement(body.getDocumentType()));
    entity.setUploaderId(currentUser.getId());
    entity.setUploaderName(currentUser.getName());
    entity.setRemark(trimToNull(body.getRemark()));
  }

  private void applySurvey(SiteSurveyRecord record, SiteSurveyUpsertDto body) {
    BigDecimal measured =
        body.getMeasuredVolume() != null ? body.getMeasuredVolume() : BigDecimal.ZERO;
    BigDecimal deduction =
        body.getDeductionVolume() != null ? body.getDeductionVolume() : BigDecimal.ZERO;
    record.setSurveyNo(body.getSurveyNo().trim());
    record.setSurveyDate(LocalDate.parse(body.getSurveyDate()));
    record.setMeasuredVolume(measured);
    record.setDeductionVolume(deduction);
    record.setSettlementVolume(measured.subtract(deduction).max(BigDecimal.ZERO));
    record.setSurveyCompany(trimToNull(body.getSurveyCompany()));
    record.setSurveyorName(trimToNull(body.getSurveyorName()));
    record.setStatus(StringUtils.hasText(body.getStatus()) ? body.getStatus().trim().toUpperCase() : "DRAFT");
    record.setReportUrl(trimToNull(body.getReportUrl()));
    record.setRemark(trimToNull(body.getRemark()));
  }

  private void validatePersonnel(
      SitePersonnelUpsertDto body, Long tenantId, Long siteId, Long currentId) {
    if (body.getUserId() == null) {
      throw new BizException(400, "请选择人员账号");
    }
    Long existing =
        sitePersonnelConfigMapper.selectCount(
            new LambdaQueryWrapper<SitePersonnelConfig>()
                .eq(SitePersonnelConfig::getTenantId, tenantId)
                .eq(SitePersonnelConfig::getSiteId, siteId)
                .eq(SitePersonnelConfig::getUserId, body.getUserId())
                .ne(currentId != null, SitePersonnelConfig::getId, currentId));
    if (existing != null && existing > 0) {
      throw new BizException(400, "该人员已配置到当前场地");
    }
  }

  private void applyPersonnel(SitePersonnelConfig config, SitePersonnelUpsertDto body, User targetUser) {
    config.setUserId(targetUser.getId());
    config.setOrgId(targetUser.getMainOrgId());
    config.setRoleType(
        StringUtils.hasText(body.getRoleType()) ? body.getRoleType().trim().toUpperCase() : "SITE_MANAGER");
    config.setDutyScope(StringUtils.hasText(body.getDutyScope()) ? body.getDutyScope().trim() : null);
    config.setShiftGroup(StringUtils.hasText(body.getShiftGroup()) ? body.getShiftGroup().trim() : null);
    config.setAccountEnabled(Boolean.TRUE.equals(body.getAccountEnabled()) ? 1 : 0);
    config.setRemark(StringUtils.hasText(body.getRemark()) ? body.getRemark().trim() : null);
  }

  private SiteDeviceDto toDeviceDto(SiteDevice device) {
    return new SiteDeviceDto(
        device.getId() != null ? String.valueOf(device.getId()) : null,
        device.getDeviceCode(),
        device.getDeviceName(),
        device.getDeviceType(),
        device.getProvider(),
        device.getIpAddress(),
        device.getStatus(),
        device.getLng(),
        device.getLat(),
        device.getRemark());
  }

  private List<SiteDocumentDto> loadDocumentDtos(Long tenantId, Site site, String stageCode) {
    return siteDocumentMapper.selectList(
            new LambdaQueryWrapper<SiteDocument>()
                .eq(SiteDocument::getTenantId, tenantId)
                .eq(SiteDocument::getSiteId, site.getId())
                .eq(StringUtils.hasText(stageCode), SiteDocument::getStageCode, normalizeCode(stageCode))
                .orderByAsc(SiteDocument::getStageCode)
                .orderByAsc(SiteDocument::getDocumentType)
                .orderByDesc(SiteDocument::getUpdateTime)
                .orderByDesc(SiteDocument::getId))
        .stream()
        .map(item -> toDocumentDto(item, site))
        .toList();
  }

  private List<SiteSurveyRecordDto> loadSurveyDtos(Long tenantId, Site site) {
    return siteSurveyRecordMapper.selectList(
            new LambdaQueryWrapper<SiteSurveyRecord>()
                .eq(SiteSurveyRecord::getTenantId, tenantId)
                .eq(SiteSurveyRecord::getSiteId, site.getId())
                .orderByDesc(SiteSurveyRecord::getSurveyDate)
                .orderByDesc(SiteSurveyRecord::getId))
        .stream()
        .map(item -> toSurveyDto(site, item))
        .toList();
  }

  private SiteDocumentDto loadDocumentDto(Long id, Long tenantId, Site site) {
    SiteDocument entity = siteDocumentMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "场地资料不存在");
    }
    return toDocumentDto(entity, site);
  }

  private SiteDocumentDto toDocumentDto(SiteDocument entity, Site site) {
    return new SiteDocumentDto(
        entity.getId() != null ? String.valueOf(entity.getId()) : null,
        entity.getSiteId() != null ? String.valueOf(entity.getSiteId()) : null,
        site != null ? site.getName() : null,
        entity.getStageCode(),
        entity.getApprovalType(),
        entity.getDocumentType(),
        entity.getFileName(),
        entity.getFileUrl(),
        entity.getFileSize(),
        entity.getMimeType(),
        entity.getFormatRequirement(),
        entity.getUploaderId() != null ? String.valueOf(entity.getUploaderId()) : null,
        entity.getUploaderName(),
        entity.getRemark(),
        entity.getCreateTime() != null ? entity.getCreateTime().format(ISO) : null,
        entity.getUpdateTime() != null ? entity.getUpdateTime().format(ISO) : null);
  }

  private SiteSurveyRecordDto toSurveyDto(Site site, SiteSurveyRecord record) {
    return new SiteSurveyRecordDto(
        record.getId() != null ? String.valueOf(record.getId()) : null,
        site.getId() != null ? String.valueOf(site.getId()) : null,
        site.getName(),
        record.getSurveyNo(),
        record.getSurveyDate() != null ? record.getSurveyDate().toString() : null,
        record.getMeasuredVolume(),
        record.getDeductionVolume(),
        record.getSettlementVolume(),
        record.getSurveyCompany(),
        record.getSurveyorName(),
        record.getStatus(),
        record.getReportUrl(),
        record.getRemark(),
        record.getCreateTime() != null ? record.getCreateTime().format(ISO) : null,
        record.getUpdateTime() != null ? record.getUpdateTime().format(ISO) : null);
  }

  private List<SitePersonnelConfigDto> loadPersonnelDtos(Long tenantId, Long siteId) {
    List<SitePersonnelConfig> rows =
        sitePersonnelConfigMapper.selectList(
            new LambdaQueryWrapper<SitePersonnelConfig>()
                .eq(SitePersonnelConfig::getTenantId, tenantId)
                .eq(SitePersonnelConfig::getSiteId, siteId)
                .orderByDesc(SitePersonnelConfig::getAccountEnabled)
                .orderByAsc(SitePersonnelConfig::getRoleType)
                .orderByAsc(SitePersonnelConfig::getId));
    if (rows.isEmpty()) {
      return Collections.emptyList();
    }
    Map<Long, User> userMap =
        userMapper.selectBatchIds(
                rows.stream()
                    .map(SitePersonnelConfig::getUserId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new)))
            .stream()
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
    Map<Long, Org> orgMap =
        loadOrgMap(
            rows.stream()
                .map(SitePersonnelConfig::getOrgId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    return rows.stream().map(item -> toPersonnelDto(item, userMap, orgMap)).toList();
  }

  private SitePersonnelConfigDto loadPersonnelDto(Long id, Long tenantId) {
    SitePersonnelConfig entity = sitePersonnelConfigMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "场地人员配置不存在");
    }
    User user = entity.getUserId() != null ? userMapper.selectById(entity.getUserId()) : null;
    Map<Long, Org> orgMap = loadOrgMap(Collections.singletonList(entity.getOrgId()));
    return toPersonnelDto(
        entity,
        user == null || user.getId() == null ? Collections.emptyMap() : Map.of(user.getId(), user),
        orgMap);
  }

  private SitePersonnelConfigDto toPersonnelDto(
      SitePersonnelConfig config, Map<Long, User> userMap, Map<Long, Org> orgMap) {
    User user = config.getUserId() != null ? userMap.get(config.getUserId()) : null;
    Org org = config.getOrgId() != null ? orgMap.get(config.getOrgId()) : null;
    return new SitePersonnelConfigDto(
        config.getId() != null ? String.valueOf(config.getId()) : null,
        config.getUserId() != null ? String.valueOf(config.getUserId()) : null,
        user != null ? user.getUsername() : null,
        user != null ? user.getName() : "已删除用户",
        user != null ? user.getMobile() : null,
        user != null ? user.getUserType() : null,
        config.getOrgId() != null ? String.valueOf(config.getOrgId()) : null,
        org != null ? org.getOrgName() : null,
        config.getRoleType(),
        config.getDutyScope(),
        config.getShiftGroup(),
        Objects.equals(config.getAccountEnabled(), 1),
        config.getRemark(),
        config.getCreateTime() != null ? config.getCreateTime().format(ISO) : null,
        config.getUpdateTime() != null ? config.getUpdateTime().format(ISO) : null);
  }

  private Map<Long, Org> loadOrgMap(Collection<Long> orgIds) {
    if (orgIds == null || orgIds.isEmpty()) {
      return Collections.emptyMap();
    }
    List<Long> effectiveOrgIds =
        orgIds.stream().filter(Objects::nonNull).distinct().toList();
    if (effectiveOrgIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return orgMapper.selectBatchIds(effectiveOrgIds).stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(Org::getId, Function.identity(), (left, right) -> left));
  }

  private SiteDocument requireDocument(Long documentId, Long tenantId, Long siteId) {
    SiteDocument entity = siteDocumentMapper.selectById(documentId);
    if (entity == null
        || !Objects.equals(entity.getTenantId(), tenantId)
        || !Objects.equals(entity.getSiteId(), siteId)) {
      throw new BizException(404, "场地资料不存在");
    }
    return entity;
  }

  private SiteSurveyRecord requireSurvey(Long surveyId, Long tenantId, Long siteId) {
    SiteSurveyRecord entity = siteSurveyRecordMapper.selectById(surveyId);
    if (entity == null
        || !Objects.equals(entity.getTenantId(), tenantId)
        || !Objects.equals(entity.getSiteId(), siteId)) {
      throw new BizException(404, "场地测绘记录不存在");
    }
    return entity;
  }

  private SitePersonnelConfig requirePersonnel(Long personnelId, Long tenantId, Long siteId) {
    SitePersonnelConfig config = sitePersonnelConfigMapper.selectById(personnelId);
    if (config == null
        || !Objects.equals(config.getTenantId(), tenantId)
        || !Objects.equals(config.getSiteId(), siteId)) {
      throw new BizException(404, "场地人员配置不存在");
    }
    return config;
  }

  private User requireTargetUser(Long userId, Long tenantId) {
    if (userId == null) {
      throw new BizException(400, "请选择人员账号");
    }
    User targetUser = userMapper.selectById(userId);
    if (targetUser == null || !Objects.equals(targetUser.getTenantId(), tenantId)) {
      throw new BizException(404, "人员账号不存在");
    }
    return targetUser;
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private String normalizeCode(String value) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
  }

  private String resolveFormatRequirement(String documentType) {
    String code = normalizeCode(documentType);
    if ("PROJECT_APPROVAL".equals(code)
        || "EIA_APPROVAL".equals(code)
        || "LAND_LEASE".equals(code)
        || "BUSINESS_LICENSE".equals(code)
        || "CONSTRUCTION_PLAN".equals(code)
        || "COMPLETION_ACCEPTANCE".equals(code)
        || "TRANSFER_ACCEPTANCE".equals(code)
        || "SITE_SETTLEMENT_ARCHIVE".equals(code)) {
      return "PDF";
    }
    if ("BOUNDARY_SURVEY".equals(code)) {
      return "PDF,JPG,PNG";
    }
    if ("SAFETY_INSPECTION".equals(code)) {
      return "DOCX,PDF";
    }
    if ("OPERATION_LEDGER".equals(code)) {
      return "XLSX,PDF";
    }
    if ("WEIGHBRIDGE_RECORD".equals(code)) {
      return "XLSX,CSV";
    }
    if ("SITE_ARCHIVE".equals(code)) {
      return "ZIP,PDF";
    }
    return "PDF,DOC,DOCX,XLS,XLSX,CSV,JPG,PNG,ZIP";
  }

  private boolean isSupportedDocumentStage(String stageCode) {
    return "APPROVAL".equals(stageCode)
        || "CONSTRUCTION".equals(stageCode)
        || "OPERATION".equals(stageCode)
        || "TRANSFER".equals(stageCode);
  }

  private String resolveDocumentStage(String documentType) {
    String code = normalizeCode(documentType);
    if ("PROJECT_APPROVAL".equals(code)
        || "EIA_APPROVAL".equals(code)
        || "LAND_LEASE".equals(code)
        || "BUSINESS_LICENSE".equals(code)) {
      return "APPROVAL";
    }
    if ("CONSTRUCTION_PLAN".equals(code)
        || "BOUNDARY_SURVEY".equals(code)
        || "COMPLETION_ACCEPTANCE".equals(code)) {
      return "CONSTRUCTION";
    }
    if ("SAFETY_INSPECTION".equals(code)
        || "OPERATION_LEDGER".equals(code)
        || "WEIGHBRIDGE_RECORD".equals(code)) {
      return "OPERATION";
    }
    if ("TRANSFER_ACCEPTANCE".equals(code)
        || "SITE_ARCHIVE".equals(code)
        || "SITE_SETTLEMENT_ARCHIVE".equals(code)) {
      return "TRANSFER";
    }
    return null;
  }

  private String resolveApprovalType(String approvalType, String documentType) {
    String normalized = normalizeCode(approvalType);
    if (StringUtils.hasText(normalized)) {
      return normalized;
    }
    String code = normalizeCode(documentType);
    if ("PROJECT_APPROVAL".equals(code)) {
      return "PROJECT";
    }
    if ("EIA_APPROVAL".equals(code)) {
      return "EIA";
    }
    if ("LAND_LEASE".equals(code)) {
      return "LAND";
    }
    if ("BUSINESS_LICENSE".equals(code)) {
      return "LICENSE";
    }
    if ("CONSTRUCTION_PLAN".equals(code)
        || "BOUNDARY_SURVEY".equals(code)
        || "COMPLETION_ACCEPTANCE".equals(code)) {
      return "CONSTRUCTION";
    }
    if ("SAFETY_INSPECTION".equals(code)
        || "OPERATION_LEDGER".equals(code)
        || "WEIGHBRIDGE_RECORD".equals(code)) {
      return "SAFETY";
    }
    if ("TRANSFER_ACCEPTANCE".equals(code)
        || "SITE_ARCHIVE".equals(code)
        || "SITE_SETTLEMENT_ARCHIVE".equals(code)) {
      return "TRANSFER";
    }
    return null;
  }

  private String extractExtension(String fileName) {
    int index = fileName.lastIndexOf('.');
    if (index < 0 || index == fileName.length() - 1) {
      return "";
    }
    return fileName.substring(index + 1).trim().toUpperCase();
  }

  private boolean matchesFormat(String extension, String requiredFormat) {
    if (!StringUtils.hasText(requiredFormat)) {
      return true;
    }
    for (String item : requiredFormat.split(",")) {
      if (extension.equalsIgnoreCase(item.trim())) {
        return true;
      }
    }
    return false;
  }

  private Site requireSite(Long siteId) {
    Site site = siteMapper.selectById(siteId);
    if (site == null) {
      throw new BizException(404, "场地不存在");
    }
    return site;
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }
}
