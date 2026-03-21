package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.vehicle.VehicleModel;
import com.xngl.infrastructure.persistence.mapper.VehicleModelMapper;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.user.StatusUpdateDto;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicle-models")
public class VehicleModelsController {

  private final VehicleModelMapper vehicleModelMapper;
  private final UserService userService;

  public VehicleModelsController(VehicleModelMapper vehicleModelMapper, UserService userService) {
    this.vehicleModelMapper = vehicleModelMapper;
    this.userService = userService;
  }

  @GetMapping
  public ApiResult<List<VehicleModel>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    String keywordValue = trimToNull(keyword);
    String statusValue = trimToNull(status);
    List<VehicleModel> rows =
        vehicleModelMapper.selectList(
            new LambdaQueryWrapper<VehicleModel>()
                .eq(VehicleModel::getTenantId, currentUser.getTenantId())
                .eq(StringUtils.hasText(statusValue), VehicleModel::getStatus, statusValue)
                .and(
                    StringUtils.hasText(keywordValue),
                    wrapper ->
                        wrapper
                            .like(VehicleModel::getModelCode, keywordValue)
                            .or()
                            .like(VehicleModel::getBrand, keywordValue)
                            .or()
                            .like(VehicleModel::getModelName, keywordValue)
                            .or()
                            .like(VehicleModel::getVehicleType, keywordValue))
                .orderByAsc(VehicleModel::getBrand)
                .orderByAsc(VehicleModel::getModelName)
                .orderByAsc(VehicleModel::getId));
    return ApiResult.ok(rows);
  }

  @GetMapping("/{id}")
  public ApiResult<VehicleModel> get(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    VehicleModel entity = requireEntity(id, currentUser.getTenantId());
    return ApiResult.ok(entity);
  }

  @PostMapping
  public ApiResult<VehicleModel> create(
      @RequestBody VehicleModelUpsertRequest body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validate(body, currentUser.getTenantId(), null);
    VehicleModel entity = new VehicleModel();
    entity.setTenantId(currentUser.getTenantId());
    apply(entity, body);
    entity.setStatus(defaultStatus(body.getStatus()));
    vehicleModelMapper.insert(entity);
    return ApiResult.ok(vehicleModelMapper.selectById(entity.getId()));
  }

  @PutMapping("/{id}")
  public ApiResult<VehicleModel> update(
      @PathVariable Long id, @RequestBody VehicleModelUpsertRequest body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    VehicleModel entity = requireEntity(id, currentUser.getTenantId());
    validate(body, currentUser.getTenantId(), id);
    apply(entity, body);
    if (StringUtils.hasText(body.getStatus())) {
      entity.setStatus(defaultStatus(body.getStatus()));
    }
    vehicleModelMapper.updateById(entity);
    return ApiResult.ok(vehicleModelMapper.selectById(id));
  }

  @PutMapping("/{id}/status")
  public ApiResult<Void> updateStatus(
      @PathVariable Long id, @RequestBody StatusUpdateDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    VehicleModel entity = requireEntity(id, currentUser.getTenantId());
    entity.setStatus(defaultStatus(body != null ? body.getStatus() : null));
    vehicleModelMapper.updateById(entity);
    return ApiResult.ok();
  }

  @DeleteMapping("/{id}")
  public ApiResult<Void> delete(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    requireEntity(id, currentUser.getTenantId());
    vehicleModelMapper.deleteById(id);
    return ApiResult.ok();
  }

  private void validate(VehicleModelUpsertRequest body, Long tenantId, Long currentId) {
    if (body == null
        || !StringUtils.hasText(body.getModelCode())
        || !StringUtils.hasText(body.getBrand())
        || !StringUtils.hasText(body.getModelName())) {
      throw new BizException(400, "车型编码、品牌、车型名称不能为空");
    }
    VehicleModel existing =
        vehicleModelMapper.selectOne(
            new LambdaQueryWrapper<VehicleModel>()
                .eq(VehicleModel::getTenantId, tenantId)
                .eq(VehicleModel::getModelCode, body.getModelCode().trim()));
    if (existing != null && !Objects.equals(existing.getId(), currentId)) {
      throw new BizException(400, "车型编码已存在");
    }
  }

  private void apply(VehicleModel entity, VehicleModelUpsertRequest body) {
    entity.setModelCode(body.getModelCode().trim());
    entity.setBrand(body.getBrand().trim());
    entity.setModelName(body.getModelName().trim());
    entity.setVehicleType(trimToNull(body.getVehicleType()));
    entity.setAxleCount(body.getAxleCount());
    entity.setSeatCount(body.getSeatCount());
    entity.setDeadWeight(defaultDecimal(body.getDeadWeight()));
    entity.setLoadWeight(defaultDecimal(body.getLoadWeight()));
    entity.setEnergyType(trimToNull(body.getEnergyType()));
    entity.setRemark(trimToNull(body.getRemark()));
  }

  private BigDecimal defaultDecimal(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private VehicleModel requireEntity(Long id, Long tenantId) {
    VehicleModel entity = vehicleModelMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "车型不存在");
    }
    return entity;
  }

  private String defaultStatus(String status) {
    return StringUtils.hasText(status) ? status.trim().toUpperCase() : "ENABLED";
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
      if (user == null || user.getTenantId() == null) {
        throw new BizException(401, "用户不存在");
      }
      return user;
    } catch (NumberFormatException ex) {
      throw new BizException(401, "token 中的用户信息无效");
    }
  }

  @Data
  public static class VehicleModelUpsertRequest {
    private String modelCode;
    private String brand;
    private String modelName;
    private String vehicleType;
    private Integer axleCount;
    private Integer seatCount;
    private BigDecimal deadWeight;
    private BigDecimal loadWeight;
    private String energyType;
    private String status;
    private String remark;
  }
}
