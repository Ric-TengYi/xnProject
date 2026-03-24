package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.vehicle.VehicleModel;
import com.xngl.infrastructure.persistence.mapper.VehicleModelMapper;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.user.StatusUpdateDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
  private final UserContext userContext;

  public VehicleModelsController(VehicleModelMapper vehicleModelMapper, UserContext userContext) {
    this.vehicleModelMapper = vehicleModelMapper;
    this.userContext = userContext;
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

  @GetMapping("/export")
  public ResponseEntity<byte[]> export(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<VehicleModel> rows =
        vehicleModelMapper.selectList(
            new LambdaQueryWrapper<VehicleModel>()
                .eq(VehicleModel::getTenantId, currentUser.getTenantId())
                .eq(StringUtils.hasText(trimToNull(status)), VehicleModel::getStatus, trimToNull(status))
                .and(
                    StringUtils.hasText(trimToNull(keyword)),
                    wrapper ->
                        wrapper
                            .like(VehicleModel::getModelCode, trimToNull(keyword))
                            .or()
                            .like(VehicleModel::getBrand, trimToNull(keyword))
                            .or()
                            .like(VehicleModel::getModelName, trimToNull(keyword))
                            .or()
                            .like(VehicleModel::getVehicleType, trimToNull(keyword)))
                .orderByAsc(VehicleModel::getBrand)
                .orderByAsc(VehicleModel::getModelName)
                .orderByAsc(VehicleModel::getId));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=vehicle_models.csv")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(buildCsv(rows).getBytes(StandardCharsets.UTF_8));
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

  private String buildCsv(List<VehicleModel> rows) {
    StringBuilder builder =
        new StringBuilder("车型编码,品牌,车型名称,车辆类型,轴数,座位数,自重(吨),载重(吨),能源类型,状态,备注\n");
    for (VehicleModel row : rows) {
      builder
          .append(csv(row.getModelCode())).append(',')
          .append(csv(row.getBrand())).append(',')
          .append(csv(row.getModelName())).append(',')
          .append(csv(row.getVehicleType())).append(',')
          .append(row.getAxleCount() != null ? row.getAxleCount() : "").append(',')
          .append(row.getSeatCount() != null ? row.getSeatCount() : "").append(',')
          .append(decimalText(row.getDeadWeight())).append(',')
          .append(decimalText(row.getLoadWeight())).append(',')
          .append(csv(row.getEnergyType())).append(',')
          .append(csv(row.getStatus())).append(',')
          .append(csv(row.getRemark())).append('\n');
    }
    return builder.toString();
  }

  private String csv(String value) {
    if (value == null) {
      return "";
    }
    String escaped = value.replace("\"", "\"\"");
    if (escaped.contains(",") || escaped.contains("\n")) {
      return "\"" + escaped + "\"";
    }
    return escaped;
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }

  private String decimalText(BigDecimal value) {
    return value != null ? value.stripTrailingZeros().toPlainString() : "";
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
