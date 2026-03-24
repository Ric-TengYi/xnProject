package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.manager.disposal.entity.DisposalPermit;
import com.xngl.manager.disposal.mapper.DisposalPermitMapper;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.Data;
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
@RequestMapping("/api/disposal-permits")
public class DisposalPermitsController {

  private final DisposalPermitMapper permitMapper;
  private final ContractMapper contractMapper;
  private final ProjectMapper projectMapper;
  private final SiteMapper siteMapper;
  private final VehicleMapper vehicleMapper;
  private final UserContext userContext;

  public DisposalPermitsController(
      DisposalPermitMapper permitMapper,
      ContractMapper contractMapper,
      ProjectMapper projectMapper,
      SiteMapper siteMapper,
      VehicleMapper vehicleMapper,
      UserContext userContext) {
    this.permitMapper = permitMapper;
    this.contractMapper = contractMapper;
    this.projectMapper = projectMapper;
    this.siteMapper = siteMapper;
    this.vehicleMapper = vehicleMapper;
    this.userContext = userContext;
  }

  @GetMapping
  public ApiResult<List<DisposalPermit>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String permitType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long contractId,
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) Long siteId,
      @RequestParam(required = false) String vehicleNo,
      @RequestParam(required = false) String bindStatus,
      @RequestParam(required = false) String sourcePlatform,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    String keywordValue = trimToNull(keyword);
    String typeValue = trimToNull(permitType);
    String statusValue = trimToNull(status);
    String vehicleNoValue = normalizePlateNo(vehicleNo);
    String bindStatusValue = normalizeUpper(bindStatus);
    String sourcePlatformValue = normalizeUpper(sourcePlatform);
    List<DisposalPermit> rows =
        permitMapper.selectList(
            new LambdaQueryWrapper<DisposalPermit>()
                .eq(DisposalPermit::getTenantId, currentUser.getTenantId())
                .and(
                    StringUtils.hasText(keywordValue),
                    wrapper ->
                        wrapper
                            .like(DisposalPermit::getPermitNo, keywordValue)
                            .or()
                            .like(DisposalPermit::getVehicleNo, keywordValue)
                            .or()
                            .like(DisposalPermit::getExternalRefNo, keywordValue))
                .eq(StringUtils.hasText(typeValue), DisposalPermit::getPermitType, typeValue)
                .eq(StringUtils.hasText(statusValue), DisposalPermit::getStatus, statusValue)
                .eq(contractId != null, DisposalPermit::getContractId, contractId)
                .eq(projectId != null, DisposalPermit::getProjectId, projectId)
                .eq(siteId != null, DisposalPermit::getSiteId, siteId)
                .eq(StringUtils.hasText(vehicleNoValue), DisposalPermit::getVehicleNo, vehicleNoValue)
                .eq(StringUtils.hasText(bindStatusValue), DisposalPermit::getBindStatus, bindStatusValue)
                .eq(
                    StringUtils.hasText(sourcePlatformValue),
                    DisposalPermit::getSourcePlatform,
                    sourcePlatformValue));
    rows.sort(
        Comparator.comparing(DisposalPermit::getUpdateTime, Comparator.nullsLast(Comparator.naturalOrder()))
            .reversed()
            .thenComparing(DisposalPermit::getId, Comparator.nullsLast(Comparator.reverseOrder())));
    return ApiResult.ok(rows);
  }

  @GetMapping("/{id}")
  public ApiResult<DisposalPermit> get(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    DisposalPermit permit = permitMapper.selectById(id);
    if (permit == null || !Objects.equals(permit.getTenantId(), currentUser.getTenantId())) {
      return ApiResult.fail(404, "处置证不存在");
    }
    return ApiResult.ok(permit);
  }

  @PostMapping
  public ApiResult<DisposalPermit> create(
      @RequestBody PermitUpsertRequest body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validate(body, currentUser.getTenantId(), null);
    DisposalPermit permit = new DisposalPermit();
    permit.setTenantId(currentUser.getTenantId());
    apply(permit, body);
    refreshComputedStatus(permit);
    permitMapper.insert(permit);
    return ApiResult.ok(permitMapper.selectById(permit.getId()));
  }

  @PutMapping("/{id}")
  public ApiResult<DisposalPermit> update(
      @PathVariable Long id, @RequestBody PermitUpsertRequest body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    DisposalPermit permit = permitMapper.selectById(id);
    if (permit == null || !Objects.equals(permit.getTenantId(), currentUser.getTenantId())) {
      return ApiResult.fail(404, "处置证不存在");
    }
    validate(body, currentUser.getTenantId(), id);
    apply(permit, body);
    refreshComputedStatus(permit);
    permitMapper.updateById(permit);
    return ApiResult.ok(permitMapper.selectById(id));
  }

  private void validate(PermitUpsertRequest body, Long tenantId, Long currentId) {
    if (body == null) {
      throw new BizException(400, "请求参数不能为空");
    }
    if (!StringUtils.hasText(body.getPermitNo())) {
      throw new BizException(400, "处置证号不能为空");
    }
    DisposalPermit existing =
        permitMapper.selectOne(
            new LambdaQueryWrapper<DisposalPermit>()
                .eq(DisposalPermit::getTenantId, tenantId)
                .eq(DisposalPermit::getPermitNo, body.getPermitNo().trim()));
    if (existing != null && !Objects.equals(existing.getId(), currentId)) {
      throw new BizException(400, "处置证号已存在");
    }
    if (body.getIssueDate() != null && body.getExpireDate() != null
        && body.getExpireDate().isBefore(body.getIssueDate())) {
      throw new BizException(400, "到期日期不能早于签发日期");
    }
    if (body.getApprovedVolume() != null
        && body.getUsedVolume() != null
        && body.getUsedVolume().compareTo(body.getApprovedVolume()) > 0) {
      throw new BizException(400, "已用方量不能大于核准方量");
    }

    Contract contract = resolveContract(tenantId, body.getContractId());
    Project project = resolveProject(body.getProjectId());
    Site site = resolveSite(body.getSiteId());
    Vehicle vehicle = resolveVehicle(tenantId, body.getVehicleNo());

    if (contract != null) {
      if (body.getProjectId() != null && !Objects.equals(contract.getProjectId(), body.getProjectId())) {
        throw new BizException(400, "处置证关联合同与项目不匹配");
      }
      if (body.getSiteId() != null && !Objects.equals(contract.getSiteId(), body.getSiteId())) {
        throw new BizException(400, "处置证关联合同与场地不匹配");
      }
    }

    if (site != null && body.getProjectId() != null && site.getProjectId() != null
        && !Objects.equals(site.getProjectId(), body.getProjectId())) {
      throw new BizException(400, "处置证关联场地与项目不匹配");
    }

    if (vehicle == null && StringUtils.hasText(body.getVehicleNo())) {
      throw new BizException(400, "绑定车辆不存在");
    }

    if (project == null && body.getProjectId() != null) {
      throw new BizException(404, "关联项目不存在");
    }
    if (site == null && body.getSiteId() != null) {
      throw new BizException(404, "指定场地不存在");
    }
  }

  private void apply(DisposalPermit permit, PermitUpsertRequest body) {
    Contract contract = resolveContract(permit.getTenantId(), body.getContractId());
    Long effectiveProjectId =
        body.getProjectId() != null ? body.getProjectId() : (contract != null ? contract.getProjectId() : null);
    Long effectiveSiteId =
        body.getSiteId() != null ? body.getSiteId() : (contract != null ? contract.getSiteId() : null);
    permit.setPermitNo(body.getPermitNo().trim());
    permit.setPermitType(defaultValue(body.getPermitType(), "DISPOSAL"));
    permit.setProjectId(effectiveProjectId);
    permit.setContractId(body.getContractId());
    permit.setSiteId(effectiveSiteId);
    permit.setVehicleNo(normalizePlateNo(body.getVehicleNo()));
    permit.setIssueDate(body.getIssueDate());
    permit.setExpireDate(body.getExpireDate());
    permit.setApprovedVolume(defaultDecimal(body.getApprovedVolume()));
    permit.setUsedVolume(defaultDecimal(body.getUsedVolume()));
    permit.setBindStatus(StringUtils.hasText(body.getVehicleNo()) ? "BOUND" : "UNBOUND");
    if (!StringUtils.hasText(permit.getSourcePlatform())) {
      permit.setSourcePlatform("MANUAL");
    }
    permit.setRemark(trimToNull(body.getRemark()));
    if (StringUtils.hasText(body.getStatus())) {
      permit.setStatus(body.getStatus().trim().toUpperCase());
    }
  }

  private void refreshComputedStatus(DisposalPermit permit) {
    if (StringUtils.hasText(permit.getStatus()) && "VOID".equalsIgnoreCase(permit.getStatus())) {
      return;
    }
    if (permit.getExpireDate() == null) {
      permit.setStatus(defaultValue(permit.getStatus(), "ACTIVE"));
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

  private BigDecimal defaultDecimal(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private String defaultValue(String value, String fallback) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase() : fallback;
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private String normalizeUpper(String value) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
  }

  private String normalizePlateNo(String value) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
  }

  private Contract resolveContract(Long tenantId, Long contractId) {
    if (contractId == null) {
      return null;
    }
    Contract contract = contractMapper.selectById(contractId);
    if (contract == null || !Objects.equals(contract.getTenantId(), tenantId)) {
      throw new BizException(404, "关联合同不存在");
    }
    return contract;
  }

  private Project resolveProject(Long projectId) {
    return projectId != null ? projectMapper.selectById(projectId) : null;
  }

  private Site resolveSite(Long siteId) {
    return siteId != null ? siteMapper.selectById(siteId) : null;
  }

  private Vehicle resolveVehicle(Long tenantId, String vehicleNo) {
    String normalizedVehicleNo = normalizePlateNo(vehicleNo);
    if (!StringUtils.hasText(normalizedVehicleNo)) {
      return null;
    }
    return vehicleMapper.selectOne(
        new LambdaQueryWrapper<Vehicle>()
            .eq(Vehicle::getTenantId, tenantId)
            .eq(Vehicle::getPlateNo, normalizedVehicleNo)
            .last("limit 1"));
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }

  @Data
  public static class PermitUpsertRequest {
    private String permitNo;
    private String permitType;
    private Long projectId;
    private Long contractId;
    private Long siteId;
    private String vehicleNo;
    private LocalDate issueDate;
    private LocalDate expireDate;
    private BigDecimal approvedVolume;
    private BigDecimal usedVolume;
    private String status;
    private String remark;
  }
}
