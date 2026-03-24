package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.site.DamMonitorRecord;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.site.SiteDevice;
import com.xngl.infrastructure.persistence.entity.site.WeighbridgeRecord;
import com.xngl.infrastructure.persistence.entity.system.PlatformIntegrationConfig;
import com.xngl.infrastructure.persistence.entity.system.PlatformSyncLog;
import com.xngl.infrastructure.persistence.entity.system.SsoLoginTicket;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.DamMonitorRecordMapper;
import com.xngl.infrastructure.persistence.mapper.PlatformIntegrationConfigMapper;
import com.xngl.infrastructure.persistence.mapper.PlatformSyncLogMapper;
import com.xngl.infrastructure.persistence.mapper.SiteDeviceMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.infrastructure.persistence.mapper.SsoLoginTicketMapper;
import com.xngl.infrastructure.persistence.mapper.WeighbridgeRecordMapper;
import com.xngl.manager.disposal.entity.DisposalPermit;
import com.xngl.manager.disposal.mapper.DisposalPermitMapper;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.platform.DamMonitorRecordDto;
import com.xngl.web.dto.platform.DamMonitorRecordUpsertDto;
import com.xngl.web.dto.platform.GovPermitSyncRequestDto;
import com.xngl.web.dto.platform.GovPermitSyncResultDto;
import com.xngl.web.dto.platform.PlatformIntegrationConfigDto;
import com.xngl.web.dto.platform.PlatformIntegrationConfigUpsertDto;
import com.xngl.web.dto.platform.PlatformIntegrationOverviewDto;
import com.xngl.web.dto.platform.PlatformSyncLogDto;
import com.xngl.web.dto.platform.PlatformVideoChannelDto;
import com.xngl.web.dto.platform.SsoTicketCreateDto;
import com.xngl.web.dto.platform.SsoTicketDto;
import com.xngl.web.dto.platform.WeighbridgeControlCommandDto;
import com.xngl.web.dto.platform.WeighbridgeRecordDto;
import com.xngl.web.dto.platform.WeighbridgeRecordUpsertDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
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
@RequestMapping("/api/platform-integrations")
public class PlatformIntegrationsController {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final List<String> INTEGRATION_CODES =
      List.of("SSO", "VIDEO", "DAM_MONITOR", "GOV_PORTAL", "WEIGHBRIDGE");

  private final PlatformIntegrationConfigMapper platformIntegrationConfigMapper;
  private final SsoLoginTicketMapper ssoLoginTicketMapper;
  private final SiteDeviceMapper siteDeviceMapper;
  private final SiteMapper siteMapper;
  private final DamMonitorRecordMapper damMonitorRecordMapper;
  private final PlatformSyncLogMapper platformSyncLogMapper;
  private final WeighbridgeRecordMapper weighbridgeRecordMapper;
  private final DisposalPermitMapper disposalPermitMapper;
  private final ContractMapper contractMapper;
  private final UserContext userContext;
  private final ObjectMapper objectMapper;

  public PlatformIntegrationsController(
      PlatformIntegrationConfigMapper platformIntegrationConfigMapper,
      SsoLoginTicketMapper ssoLoginTicketMapper,
      SiteDeviceMapper siteDeviceMapper,
      SiteMapper siteMapper,
      DamMonitorRecordMapper damMonitorRecordMapper,
      PlatformSyncLogMapper platformSyncLogMapper,
      WeighbridgeRecordMapper weighbridgeRecordMapper,
      DisposalPermitMapper disposalPermitMapper,
      ContractMapper contractMapper,
      UserContext userContext,
      ObjectMapper objectMapper) {
    this.platformIntegrationConfigMapper = platformIntegrationConfigMapper;
    this.ssoLoginTicketMapper = ssoLoginTicketMapper;
    this.siteDeviceMapper = siteDeviceMapper;
    this.siteMapper = siteMapper;
    this.damMonitorRecordMapper = damMonitorRecordMapper;
    this.platformSyncLogMapper = platformSyncLogMapper;
    this.weighbridgeRecordMapper = weighbridgeRecordMapper;
    this.disposalPermitMapper = disposalPermitMapper;
    this.contractMapper = contractMapper;
    this.userContext = userContext;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/overview")
  public ApiResult<PlatformIntegrationOverviewDto> overview(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<PlatformIntegrationConfig> configs = loadConfigs(currentUser.getTenantId());
    int enabledCount = (int) configs.stream().filter(config -> Objects.equals(config.getEnabled(), 1)).count();
    int videoChannelCount = loadVideoChannels(currentUser.getTenantId()).size();
    int activeTicketCount =
        Math.toIntExact(
            ssoLoginTicketMapper.selectCount(
                new LambdaQueryWrapper<SsoLoginTicket>()
                    .eq(SsoLoginTicket::getTenantId, currentUser.getTenantId())
                    .eq(SsoLoginTicket::getUsedFlag, 0)
                    .ge(SsoLoginTicket::getExpiresAt, LocalDateTime.now())));
    int onlineDamSiteCount =
        (int)
            damMonitorRecordMapper.selectList(
                    new LambdaQueryWrapper<DamMonitorRecord>()
                        .eq(DamMonitorRecord::getTenantId, currentUser.getTenantId())
                        .eq(DamMonitorRecord::getOnlineStatus, "ONLINE")
                        .orderByDesc(DamMonitorRecord::getMonitorTime))
                .stream()
                .map(DamMonitorRecord::getSiteId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    int govSyncCount =
        Math.toIntExact(
            platformSyncLogMapper.selectCount(
                new LambdaQueryWrapper<PlatformSyncLog>()
                    .eq(PlatformSyncLog::getTenantId, currentUser.getTenantId())
                    .eq(PlatformSyncLog::getIntegrationCode, "GOV_PORTAL")));
    int weighbridgeRecordCount =
        Math.toIntExact(
            weighbridgeRecordMapper.selectCount(
                new LambdaQueryWrapper<WeighbridgeRecord>()
                    .eq(WeighbridgeRecord::getTenantId, currentUser.getTenantId())));
    return ApiResult.ok(
        new PlatformIntegrationOverviewDto(
            enabledCount,
            configs.size(),
            videoChannelCount,
            onlineDamSiteCount,
            activeTicketCount,
            govSyncCount,
            weighbridgeRecordCount));
  }

  @GetMapping("/configs")
  public ApiResult<List<PlatformIntegrationConfigDto>> listConfigs(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    return ApiResult.ok(
        loadConfigs(currentUser.getTenantId()).stream().map(this::toConfigDto).toList());
  }

  @PutMapping("/{integrationCode}")
  public ApiResult<PlatformIntegrationConfigDto> upsertConfig(
      @PathVariable String integrationCode,
      @RequestBody PlatformIntegrationConfigUpsertDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    String effectiveCode = normalizeCode(integrationCode);
    if (!INTEGRATION_CODES.contains(effectiveCode)) {
      throw new BizException(404, "对接类型不存在");
    }
    PlatformIntegrationConfig config = requireConfig(currentUser.getTenantId(), effectiveCode);
    config.setEnabled(Boolean.TRUE.equals(body.getEnabled()) ? 1 : 0);
    config.setVendorName(trimToNull(body.getVendorName()));
    config.setBaseUrl(trimToNull(body.getBaseUrl()));
    config.setApiVersion(trimToNull(body.getApiVersion()));
    config.setClientId(trimToNull(body.getClientId()));
    config.setClientSecret(trimToNull(body.getClientSecret()));
    config.setAccessKey(trimToNull(body.getAccessKey()));
    config.setAccessSecret(trimToNull(body.getAccessSecret()));
    config.setCallbackPath(trimToNull(body.getCallbackPath()));
    config.setExtJson(normalizeJson(body.getExtJson()));
    config.setRemark(trimToNull(body.getRemark()));
    platformIntegrationConfigMapper.updateById(config);
    return ApiResult.ok(toConfigDto(platformIntegrationConfigMapper.selectById(config.getId())));
  }

  @PostMapping("/sso/tickets")
  public ApiResult<SsoTicketDto> createSsoTicket(
      @RequestBody(required = false) SsoTicketCreateDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    PlatformIntegrationConfig config = requireConfig(currentUser.getTenantId(), "SSO");
    int expireSeconds = resolveTicketExpireSeconds(config);
    LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expireSeconds);
    String ticket = UUID.randomUUID().toString().replace("-", "");

    SsoLoginTicket record = new SsoLoginTicket();
    record.setTenantId(currentUser.getTenantId());
    record.setUserId(currentUser.getId());
    record.setUsername(currentUser.getUsername());
    record.setTicket(ticket);
    record.setTargetPlatform(body != null ? trimToNull(body.getTargetPlatform()) : null);
    record.setRedirectUri(body != null ? trimToNull(body.getRedirectUri()) : null);
    record.setExpiresAt(expiresAt);
    record.setUsedFlag(0);
    ssoLoginTicketMapper.insert(record);

    String baseUrl = StringUtils.hasText(config.getBaseUrl()) ? config.getBaseUrl().trim() : "https://sso.local";
    String callbackPath =
        StringUtils.hasText(config.getCallbackPath()) ? config.getCallbackPath().trim() : "/auth/sso/callback";
    String loginUrl =
        baseUrl
            + callbackPath
            + "?ticket="
            + URLEncoder.encode(ticket, StandardCharsets.UTF_8)
            + (StringUtils.hasText(record.getRedirectUri())
                ? "&redirectUri="
                    + URLEncoder.encode(record.getRedirectUri(), StandardCharsets.UTF_8)
                : "");
    return ApiResult.ok(
        new SsoTicketDto(ticket, loginUrl, record.getTargetPlatform(), expiresAt.format(ISO)));
  }

  @GetMapping("/video/channels")
  public ApiResult<List<PlatformVideoChannelDto>> videoChannels(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    return ApiResult.ok(loadVideoChannels(currentUser.getTenantId()));
  }

  @GetMapping("/dam/records")
  public ApiResult<List<DamMonitorRecordDto>> damRecords(
      @RequestParam(required = false) Long siteId, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    return ApiResult.ok(loadDamRecords(currentUser.getTenantId(), siteId));
  }

  @PostMapping("/dam/mock-sync")
  public ApiResult<DamMonitorRecordDto> mockSyncDamRecord(
      @RequestBody DamMonitorRecordUpsertDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    if (body == null || body.getSiteId() == null) {
      throw new BizException(400, "请选择场地");
    }
    Site site = siteMapper.selectById(body.getSiteId());
    if (site == null || Objects.equals(site.getDeleted(), 1)) {
      throw new BizException(404, "场地不存在");
    }
    DamMonitorRecord record = new DamMonitorRecord();
    record.setTenantId(currentUser.getTenantId());
    record.setSiteId(body.getSiteId());
    record.setIntegrationCode("DAM_MONITOR");
    record.setDeviceName(StringUtils.hasText(body.getDeviceName()) ? body.getDeviceName().trim() : site.getName() + "-坝体监测点");
    record.setMonitorTime(parseDateTime(body.getMonitorTime()));
    record.setOnlineStatus(StringUtils.hasText(body.getOnlineStatus()) ? body.getOnlineStatus().trim().toUpperCase() : "ONLINE");
    record.setSafetyLevel(StringUtils.hasText(body.getSafetyLevel()) ? body.getSafetyLevel().trim().toUpperCase() : "NORMAL");
    record.setDisplacementValue(body.getDisplacementValue() != null ? body.getDisplacementValue() : BigDecimal.ZERO);
    record.setWaterLevel(body.getWaterLevel() != null ? body.getWaterLevel() : BigDecimal.ZERO);
    record.setRainfall(body.getRainfall() != null ? body.getRainfall() : BigDecimal.ZERO);
    record.setAlarmFlag(Boolean.TRUE.equals(body.getAlarmFlag()) ? 1 : 0);
    record.setRemark(trimToNull(body.getRemark()));
    damMonitorRecordMapper.insert(record);
    return ApiResult.ok(toDamRecordDto(record, site));
  }

  @PostMapping("/gov/mock-sync")
  public ApiResult<GovPermitSyncResultDto> mockSyncGovPermits(
      @RequestBody(required = false) GovPermitSyncRequestDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    String syncMode = normalizeCode(body != null ? body.getSyncMode() : null);
    String effectiveMode = StringUtils.hasText(syncMode) ? syncMode : "MANUAL";
    boolean includeTransport = body == null || !Boolean.FALSE.equals(body.getIncludeTransportPermits());
    String batchNo =
        "GOV-"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + "-"
            + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();

    Contract contract = resolveSyncContract(currentUser.getTenantId(), body);
    Long projectId = body != null && body.getProjectId() != null ? body.getProjectId() : (contract != null ? contract.getProjectId() : null);
    Long siteId = body != null && body.getSiteId() != null ? body.getSiteId() : (contract != null ? contract.getSiteId() : null);
    String baseVehicleNo =
        StringUtils.hasText(body != null ? body.getVehicleNo() : null)
            ? body.getVehicleNo().trim().toUpperCase()
            : "浙A" + batchNo.substring(Math.max(batchNo.length() - 5, 0));

    List<String> permitTypes = includeTransport ? List.of("DISPOSAL", "TRANSPORT") : List.of("DISPOSAL");
    int createdCount = 0;
    int updatedCount = 0;
    int successCount = 0;
    for (int index = 0; index < permitTypes.size(); index++) {
      String permitType = permitTypes.get(index);
      String externalRefNo =
          "GOV-" + permitType + "-" + (projectId != null ? projectId : 0) + "-" + (siteId != null ? siteId : 0);
      DisposalPermit permit =
          disposalPermitMapper.selectOne(
              new LambdaQueryWrapper<DisposalPermit>()
                  .eq(DisposalPermit::getTenantId, currentUser.getTenantId())
                  .eq(DisposalPermit::getExternalRefNo, externalRefNo)
                  .last("limit 1"));
      boolean created = permit == null;
      if (created) {
        permit = new DisposalPermit();
        permit.setTenantId(currentUser.getTenantId());
      }
      permit.setPermitNo("P-" + externalRefNo.replace("GOV-", ""));
      permit.setPermitType(permitType);
      permit.setProjectId(projectId);
      permit.setContractId(contract != null ? contract.getId() : (body != null ? body.getContractId() : null));
      permit.setSiteId(siteId);
      permit.setVehicleNo(index == 0 ? baseVehicleNo : baseVehicleNo + "挂");
      permit.setIssueDate(LocalDate.now());
      permit.setExpireDate(LocalDate.now().plusDays("TRANSPORT".equals(permitType) ? 15 : 30));
      permit.setApprovedVolume("TRANSPORT".equals(permitType) ? BigDecimal.valueOf(420) : BigDecimal.valueOf(860));
      permit.setUsedVolume("TRANSPORT".equals(permitType) ? BigDecimal.valueOf(120) : BigDecimal.valueOf(260));
      permit.setBindStatus(StringUtils.hasText(permit.getVehicleNo()) ? "BOUND" : "UNBOUND");
      permit.setSourcePlatform("GOV_PORTAL");
      permit.setExternalRefNo(externalRefNo);
      permit.setLastSyncTime(LocalDateTime.now());
      permit.setSyncBatchNo(batchNo);
      permit.setRemark(trimToNull(body != null ? body.getRemark() : null));
      refreshPermitStatus(permit);
      if (created) {
        disposalPermitMapper.insert(permit);
        createdCount++;
      } else {
        disposalPermitMapper.updateById(permit);
        updatedCount++;
      }
      successCount++;
    }

    GovPermitSyncResultDto result =
        new GovPermitSyncResultDto(
            batchNo,
            effectiveMode,
            permitTypes.size(),
            createdCount,
            updatedCount,
            successCount,
            0,
            LocalDateTime.now().format(ISO));
    writeSyncLog(
        currentUser,
        "GOV_PORTAL",
        effectiveMode,
        "DISPOSAL_PERMIT",
        batchNo,
        permitTypes.size(),
        successCount,
        0,
        "SUCCESS",
        body,
        result,
        trimToNull(body != null ? body.getRemark() : null));
    return ApiResult.ok(result);
  }

  @GetMapping("/gov/sync-logs")
  public ApiResult<List<PlatformSyncLogDto>> govSyncLogs(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    return ApiResult.ok(loadSyncLogs(currentUser.getTenantId(), "GOV_PORTAL"));
  }

  @GetMapping("/sync-logs")
  public ApiResult<List<PlatformSyncLogDto>> syncLogs(
      @RequestParam(required = false) String integrationCode, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    return ApiResult.ok(loadSyncLogs(currentUser.getTenantId(), normalizeCode(integrationCode)));
  }

  @GetMapping("/weighbridge/records")
  public ApiResult<List<WeighbridgeRecordDto>> weighbridgeRecords(
      @RequestParam(required = false) Long siteId, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<WeighbridgeRecord> rows =
        weighbridgeRecordMapper.selectList(
            new LambdaQueryWrapper<WeighbridgeRecord>()
                .eq(WeighbridgeRecord::getTenantId, currentUser.getTenantId())
                .eq(siteId != null, WeighbridgeRecord::getSiteId, siteId)
                .orderByDesc(WeighbridgeRecord::getWeighTime)
                .orderByDesc(WeighbridgeRecord::getId));
    return ApiResult.ok(toWeighbridgeDtos(rows));
  }

  @PostMapping("/weighbridge/mock-sync")
  public ApiResult<WeighbridgeRecordDto> mockSyncWeighbridgeRecord(
      @RequestBody WeighbridgeRecordUpsertDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    if (body == null || body.getSiteId() == null || !StringUtils.hasText(body.getVehicleNo())) {
      throw new BizException(400, "场地和车牌不能为空");
    }
    Site site = siteMapper.selectById(body.getSiteId());
    if (site == null || Objects.equals(site.getDeleted(), 1)) {
      throw new BizException(404, "场地不存在");
    }
    SiteDevice device = resolveWeighbridgeDevice(currentUser.getTenantId(), body.getSiteId(), body.getDeviceId());
    WeighbridgeRecord record = new WeighbridgeRecord();
    record.setTenantId(currentUser.getTenantId());
    record.setSiteId(body.getSiteId());
    record.setDeviceId(device != null ? device.getId() : body.getDeviceId());
    record.setVehicleNo(body.getVehicleNo().trim().toUpperCase());
    record.setTicketNo(
        StringUtils.hasText(body.getTicketNo())
            ? body.getTicketNo().trim()
            : "WB-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
    record.setGrossWeight(body.getGrossWeight() != null ? body.getGrossWeight() : BigDecimal.valueOf(32.6));
    record.setTareWeight(body.getTareWeight() != null ? body.getTareWeight() : BigDecimal.valueOf(12.4));
    record.setNetWeight(
        body.getNetWeight() != null
            ? body.getNetWeight()
            : record.getGrossWeight().subtract(record.getTareWeight()).max(BigDecimal.ZERO));
    record.setEstimatedVolume(
        body.getEstimatedVolume() != null
            ? body.getEstimatedVolume()
            : record.getNetWeight().divide(BigDecimal.valueOf(1.6), 2, java.math.RoundingMode.HALF_UP));
    record.setWeighTime(parseDateTime(body.getWeighTime()));
    record.setSyncStatus(normalizeCode(body.getSyncStatus()) != null ? normalizeCode(body.getSyncStatus()) : "SYNCED");
    record.setControlCommand(trimToNull(body.getControlCommand()));
    record.setIntegrationCode("WEIGHBRIDGE");
    record.setSourceType(normalizeCode(body.getSourceType()) != null ? normalizeCode(body.getSourceType()) : "DEVICE");
    record.setRemark(trimToNull(body.getRemark()));
    weighbridgeRecordMapper.insert(record);
    writeSyncLog(
        currentUser,
        "WEIGHBRIDGE",
        "MANUAL",
        "WEIGHBRIDGE_RECORD",
        record.getTicketNo(),
        1,
        1,
        0,
        "SUCCESS",
        body,
        record,
        record.getRemark());
    return ApiResult.ok(toWeighbridgeDto(record, site, device));
  }

  @PostMapping("/weighbridge/control-command")
  public ApiResult<PlatformSyncLogDto> issueWeighbridgeControlCommand(
      @RequestBody WeighbridgeControlCommandDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    if (body == null || body.getSiteId() == null || !StringUtils.hasText(body.getCommand())) {
      throw new BizException(400, "场地和控制命令不能为空");
    }
    String batchNo =
        "CTRL-"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + "-"
            + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    PlatformSyncLog log =
        writeSyncLog(
            currentUser,
            "WEIGHBRIDGE",
            "MANUAL",
            "CONTROL_COMMAND",
            batchNo,
            1,
            1,
            0,
            "SUCCESS",
            body,
            Map.of("ack", "OK", "command", body.getCommand().trim().toUpperCase()),
            trimToNull(body.getRemark()));
    return ApiResult.ok(toSyncLogDto(log));
  }

  private List<PlatformIntegrationConfig> loadConfigs(Long tenantId) {
    Map<String, PlatformIntegrationConfig> configMap =
        platformIntegrationConfigMapper.selectList(
                new LambdaQueryWrapper<PlatformIntegrationConfig>()
                    .eq(PlatformIntegrationConfig::getTenantId, tenantId)
                    .in(PlatformIntegrationConfig::getIntegrationCode, INTEGRATION_CODES)
                    .orderByAsc(PlatformIntegrationConfig::getId))
            .stream()
            .collect(
                Collectors.toMap(
                    config -> normalizeCode(config.getIntegrationCode()),
                    Function.identity(),
                    (left, right) -> left,
                    LinkedHashMap::new));
    List<PlatformIntegrationConfig> results = new ArrayList<>();
    for (String code : INTEGRATION_CODES) {
      PlatformIntegrationConfig config = configMap.get(code);
      if (config == null) {
        config = buildDefaultConfig(tenantId, code);
        platformIntegrationConfigMapper.insert(config);
      }
      results.add(config);
    }
    return results;
  }

  private PlatformIntegrationConfig requireConfig(Long tenantId, String integrationCode) {
    return loadConfigs(tenantId).stream()
        .filter(config -> integrationCode.equals(normalizeCode(config.getIntegrationCode())))
        .findFirst()
        .orElseThrow(() -> new BizException(404, "对接配置不存在"));
  }

  private PlatformIntegrationConfig buildDefaultConfig(Long tenantId, String integrationCode) {
    PlatformIntegrationConfig config = new PlatformIntegrationConfig();
    config.setTenantId(tenantId);
    config.setIntegrationCode(integrationCode);
    config.setEnabled(0);
    switch (integrationCode) {
      case "GOV_PORTAL" -> {
        config.setIntegrationName("政务网数据对接");
        config.setVendorName("浙江政务服务网");
        config.setBaseUrl("https://gov.local");
        config.setApiVersion("v1");
        config.setCallbackPath("/platform/gov/callback");
      }
      case "WEIGHBRIDGE" -> {
        config.setIntegrationName("地磅数据对接");
        config.setVendorName("地磅控制器");
        config.setBaseUrl("https://weighbridge.local");
        config.setApiVersion("v1");
        config.setCallbackPath("/platform/weighbridge/callback");
      }
      case "VIDEO" -> {
        config.setIntegrationName("视频对接");
        config.setVendorName("iSecure Center");
        config.setBaseUrl("https://video.local");
        config.setApiVersion("iSecure Center V1.7.0+");
        config.setCallbackPath("/platform/video/callback");
      }
      case "DAM_MONITOR" -> {
        config.setIntegrationName("坝体监测");
        config.setVendorName("坝体安全监测设备");
        config.setBaseUrl("https://dam.local");
        config.setApiVersion("v1");
        config.setCallbackPath("/platform/dam/callback");
      }
      default -> {
        config.setIntegrationName("统一身份认证");
        config.setVendorName("统一认证中心");
        config.setBaseUrl("https://sso.local");
        config.setApiVersion("v1");
        config.setCallbackPath("/auth/sso/callback");
      }
    }
    return config;
  }

  private PlatformIntegrationConfigDto toConfigDto(PlatformIntegrationConfig config) {
    return new PlatformIntegrationConfigDto(
        normalizeCode(config.getIntegrationCode()),
        config.getIntegrationName(),
        Objects.equals(config.getEnabled(), 1),
        config.getVendorName(),
        config.getBaseUrl(),
        config.getApiVersion(),
        config.getClientId(),
        config.getClientSecret(),
        config.getAccessKey(),
        config.getAccessSecret(),
        config.getCallbackPath(),
        config.getExtJson(),
        config.getRemark(),
        config.getUpdateTime() != null ? config.getUpdateTime().format(ISO) : null);
  }

  private List<PlatformVideoChannelDto> loadVideoChannels(Long tenantId) {
    PlatformIntegrationConfig config = requireConfig(tenantId, "VIDEO");
    String baseUrl = StringUtils.hasText(config.getBaseUrl()) ? config.getBaseUrl().trim() : "https://video.local";
    List<SiteDevice> devices =
        siteDeviceMapper.selectList(
            new LambdaQueryWrapper<SiteDevice>()
                .eq(SiteDevice::getTenantId, tenantId)
                .in(SiteDevice::getDeviceType, Arrays.asList("VIDEO_CAMERA", "CAPTURE_CAMERA"))
                .orderByAsc(SiteDevice::getSiteId)
                .orderByAsc(SiteDevice::getId));
    if (devices.isEmpty()) {
      return Collections.emptyList();
    }
    Map<Long, Site> siteMap =
        siteMapper.selectBatchIds(
                devices.stream()
                    .map(SiteDevice::getSiteId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new)))
            .stream()
            .filter(site -> site.getId() != null)
            .collect(Collectors.toMap(Site::getId, Function.identity(), (left, right) -> left));
    return devices.stream()
        .map(
            device -> {
              Site site = device.getSiteId() != null ? siteMap.get(device.getSiteId()) : null;
              return new PlatformVideoChannelDto(
                  site != null && site.getId() != null ? String.valueOf(site.getId()) : null,
                  site != null ? site.getName() : null,
                  device.getId() != null ? String.valueOf(device.getId()) : null,
                  device.getDeviceCode(),
                  device.getDeviceName(),
                  device.getDeviceType(),
                  device.getStatus(),
                  baseUrl + "/preview/" + (StringUtils.hasText(device.getDeviceCode()) ? device.getDeviceCode() : device.getId()));
            })
        .toList();
  }

  private List<DamMonitorRecordDto> loadDamRecords(Long tenantId, Long siteId) {
    List<DamMonitorRecord> records =
        damMonitorRecordMapper.selectList(
            new LambdaQueryWrapper<DamMonitorRecord>()
                .eq(DamMonitorRecord::getTenantId, tenantId)
                .eq(siteId != null, DamMonitorRecord::getSiteId, siteId)
                .orderByDesc(DamMonitorRecord::getMonitorTime)
                .orderByDesc(DamMonitorRecord::getId));
    if (records.isEmpty()) {
      return Collections.emptyList();
    }
    Map<Long, Site> siteMap =
        siteMapper.selectBatchIds(
                records.stream()
                    .map(DamMonitorRecord::getSiteId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new)))
            .stream()
            .filter(site -> site.getId() != null)
            .collect(Collectors.toMap(Site::getId, Function.identity(), (left, right) -> left));
    return records.stream()
        .map(record -> toDamRecordDto(record, siteMap.get(record.getSiteId())))
        .toList();
  }

  private DamMonitorRecordDto toDamRecordDto(DamMonitorRecord record, Site site) {
    return new DamMonitorRecordDto(
        record.getId() != null ? String.valueOf(record.getId()) : null,
        record.getSiteId() != null ? String.valueOf(record.getSiteId()) : null,
        site != null ? site.getName() : null,
        record.getDeviceName(),
        record.getMonitorTime() != null ? record.getMonitorTime().format(ISO) : null,
        record.getOnlineStatus(),
        record.getSafetyLevel(),
        record.getDisplacementValue(),
        record.getWaterLevel(),
        record.getRainfall(),
        Objects.equals(record.getAlarmFlag(), 1),
        record.getRemark());
  }

  private int resolveTicketExpireSeconds(PlatformIntegrationConfig config) {
    if (!StringUtils.hasText(config.getExtJson())) {
      return 300;
    }
    try {
      JsonNode node = objectMapper.readTree(config.getExtJson());
      int seconds = node.path("ticketExpireSeconds").asInt(300);
      return Math.max(seconds, 60);
    } catch (Exception ignored) {
      return 300;
    }
  }

  private String normalizeJson(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      JsonNode node = objectMapper.readTree(value.trim());
      return objectMapper.writeValueAsString(node);
    } catch (Exception ex) {
      throw new BizException(400, "扩展配置 JSON 格式错误");
    }
  }

  private LocalDateTime parseDateTime(String value) {
    if (!StringUtils.hasText(value)) {
      return LocalDateTime.now();
    }
    return LocalDateTime.parse(value.trim(), ISO);
  }

  private void refreshPermitStatus(DisposalPermit permit) {
    if (StringUtils.hasText(permit.getStatus()) && "VOID".equalsIgnoreCase(permit.getStatus())) {
      return;
    }
    if (permit.getExpireDate() == null) {
      permit.setStatus("ACTIVE");
      return;
    }
    LocalDate today = LocalDate.now();
    if (permit.getExpireDate().isBefore(today)) {
      permit.setStatus("EXPIRED");
    } else if (!permit.getExpireDate().isAfter(today.plusDays(30))) {
      permit.setStatus("EXPIRING");
    } else {
      permit.setStatus("ACTIVE");
    }
  }

  private Contract resolveSyncContract(Long tenantId, GovPermitSyncRequestDto body) {
    Long contractId = body != null ? body.getContractId() : null;
    if (contractId != null) {
      return contractMapper.selectById(contractId);
    }
    return contractMapper.selectOne(
        new LambdaQueryWrapper<Contract>()
            .eq(Contract::getTenantId, tenantId)
            .orderByAsc(Contract::getId)
            .last("limit 1"));
  }

  private List<PlatformSyncLogDto> loadSyncLogs(Long tenantId, String integrationCode) {
    return platformSyncLogMapper.selectList(
            new LambdaQueryWrapper<PlatformSyncLog>()
                .eq(PlatformSyncLog::getTenantId, tenantId)
                .eq(StringUtils.hasText(integrationCode), PlatformSyncLog::getIntegrationCode, integrationCode)
                .orderByDesc(PlatformSyncLog::getSyncTime)
                .orderByDesc(PlatformSyncLog::getId))
        .stream()
        .map(this::toSyncLogDto)
        .toList();
  }

  private PlatformSyncLog writeSyncLog(
      User currentUser,
      String integrationCode,
      String syncMode,
      String bizType,
      String batchNo,
      int totalCount,
      int successCount,
      int failCount,
      String status,
      Object requestPayload,
      Object responsePayload,
      String remark) {
    PlatformSyncLog log = new PlatformSyncLog();
    log.setTenantId(currentUser.getTenantId());
    log.setIntegrationCode(integrationCode);
    log.setSyncMode(syncMode);
    log.setBizType(bizType);
    log.setBatchNo(batchNo);
    log.setTotalCount(totalCount);
    log.setSuccessCount(successCount);
    log.setFailCount(failCount);
    log.setStatus(status);
    log.setOperatorId(currentUser.getId());
    log.setOperatorName(currentUser.getName());
    log.setRequestPayload(toJsonQuietly(requestPayload));
    log.setResponsePayload(toJsonQuietly(responsePayload));
    log.setRemark(remark);
    log.setSyncTime(LocalDateTime.now());
    platformSyncLogMapper.insert(log);
    return log;
  }

  private String toJsonQuietly(Object payload) {
    if (payload == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (Exception ex) {
      return String.valueOf(payload);
    }
  }

  private PlatformSyncLogDto toSyncLogDto(PlatformSyncLog log) {
    return new PlatformSyncLogDto(
        log.getId() != null ? String.valueOf(log.getId()) : null,
        log.getIntegrationCode(),
        log.getSyncMode(),
        log.getBizType(),
        log.getBatchNo(),
        log.getTotalCount(),
        log.getSuccessCount(),
        log.getFailCount(),
        log.getStatus(),
        log.getOperatorName(),
        log.getSyncTime() != null ? log.getSyncTime().format(ISO) : null,
        log.getRequestPayload(),
        log.getResponsePayload(),
        log.getRemark());
  }

  private SiteDevice resolveWeighbridgeDevice(Long tenantId, Long siteId, Long deviceId) {
    if (deviceId != null) {
      SiteDevice device = siteDeviceMapper.selectById(deviceId);
      if (device != null
          && Objects.equals(device.getTenantId(), tenantId)
          && "WEIGHBRIDGE".equalsIgnoreCase(device.getDeviceType())) {
        return device;
      }
      throw new BizException(404, "地磅设备不存在");
    }
    return siteDeviceMapper.selectOne(
        new LambdaQueryWrapper<SiteDevice>()
            .eq(SiteDevice::getTenantId, tenantId)
            .eq(SiteDevice::getSiteId, siteId)
            .eq(SiteDevice::getDeviceType, "WEIGHBRIDGE")
            .orderByAsc(SiteDevice::getId)
            .last("limit 1"));
  }

  private List<WeighbridgeRecordDto> toWeighbridgeDtos(List<WeighbridgeRecord> rows) {
    if (rows == null || rows.isEmpty()) {
      return Collections.emptyList();
    }
    Map<Long, Site> siteMap =
        siteMapper.selectBatchIds(
                rows.stream()
                    .map(WeighbridgeRecord::getSiteId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new)))
            .stream()
            .filter(site -> site.getId() != null)
            .collect(Collectors.toMap(Site::getId, Function.identity(), (left, right) -> left));
    Map<Long, SiteDevice> deviceMap =
        siteDeviceMapper.selectBatchIds(
                rows.stream()
                    .map(WeighbridgeRecord::getDeviceId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new)))
            .stream()
            .filter(device -> device.getId() != null)
            .collect(Collectors.toMap(SiteDevice::getId, Function.identity(), (left, right) -> left));
    return rows.stream()
        .map(row -> toWeighbridgeDto(row, siteMap.get(row.getSiteId()), deviceMap.get(row.getDeviceId())))
        .toList();
  }

  private WeighbridgeRecordDto toWeighbridgeDto(WeighbridgeRecord record, Site site, SiteDevice device) {
    return new WeighbridgeRecordDto(
        record.getId() != null ? String.valueOf(record.getId()) : null,
        record.getSiteId() != null ? String.valueOf(record.getSiteId()) : null,
        site != null ? site.getName() : null,
        device != null && device.getId() != null ? String.valueOf(device.getId()) : (record.getDeviceId() != null ? String.valueOf(record.getDeviceId()) : null),
        device != null ? device.getDeviceName() : null,
        record.getVehicleNo(),
        record.getTicketNo(),
        record.getGrossWeight(),
        record.getTareWeight(),
        record.getNetWeight(),
        record.getEstimatedVolume(),
        record.getWeighTime() != null ? record.getWeighTime().format(ISO) : null,
        record.getSyncStatus(),
        record.getControlCommand(),
        record.getSourceType(),
        record.getRemark());
  }

  private String normalizeCode(String value) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }
}
