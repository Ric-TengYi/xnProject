package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.disposal.entity.DisposalPermit;
import com.xngl.manager.disposal.mapper.DisposalPermitMapper;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.exception.BizException;
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
  private final UserService userService;

  public DisposalPermitsController(DisposalPermitMapper permitMapper, UserService userService) {
    this.permitMapper = permitMapper;
    this.userService = userService;
  }

  @GetMapping
  public ApiResult<List<DisposalPermit>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String permitType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long contractId,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    String keywordValue = trimToNull(keyword);
    String typeValue = trimToNull(permitType);
    String statusValue = trimToNull(status);
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
                            .like(DisposalPermit::getVehicleNo, keywordValue))
                .eq(StringUtils.hasText(typeValue), DisposalPermit::getPermitType, typeValue)
                .eq(StringUtils.hasText(statusValue), DisposalPermit::getStatus, statusValue)
                .eq(contractId != null, DisposalPermit::getContractId, contractId));
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
  }

  private void apply(DisposalPermit permit, PermitUpsertRequest body) {
    permit.setPermitNo(body.getPermitNo().trim());
    permit.setPermitType(defaultValue(body.getPermitType(), "DISPOSAL"));
    permit.setProjectId(body.getProjectId());
    permit.setContractId(body.getContractId());
    permit.setSiteId(body.getSiteId());
    permit.setVehicleNo(trimToNull(body.getVehicleNo()));
    permit.setIssueDate(body.getIssueDate());
    permit.setExpireDate(body.getExpireDate());
    permit.setApprovedVolume(defaultDecimal(body.getApprovedVolume()));
    permit.setUsedVolume(defaultDecimal(body.getUsedVolume()));
    permit.setBindStatus(StringUtils.hasText(body.getVehicleNo()) ? "BOUND" : "UNBOUND");
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

  private User requireCurrentUser(HttpServletRequest request) {
    String userId = (String) request.getAttribute("userId");
    if (!StringUtils.hasText(userId)) {
      throw new BizException(401, "未登录或 token 无效");
    }
    try {
      User user = userService.getById(Long.parseLong(userId));
      if (user == null) {
        throw new BizException(401, "用户不存在");
      }
      return user;
    } catch (NumberFormatException ex) {
      throw new BizException(401, "token 中的用户信息无效");
    }
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
