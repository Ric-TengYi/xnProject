package com.xngl.web.controller;

import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.manager.org.OrgService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.user.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orgs")
public class OrgsController {

  private final OrgService orgService;

  public OrgsController(OrgService orgService) {
    this.orgService = orgService;
  }

  @GetMapping("/tree")
  public ApiResult<List<OrgTreeNodeDto>> tree(
      @RequestParam(required = false) Long tenantId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status) {
    if (tenantId == null) return ApiResult.ok(Collections.emptyList());
    List<Org> list = orgService.listTree(tenantId, keyword, status);
    List<OrgTreeNodeDto> tree = buildTree(list, 0L);
    return ApiResult.ok(tree);
  }

  @GetMapping("/{id}")
  public ApiResult<OrgDetailDto> get(@PathVariable Long id) {
    Org o = orgService.getById(id);
    if (o == null) return ApiResult.fail(404, "组织不存在");
    return ApiResult.ok(toDetail(o));
  }

  @PostMapping
  public ApiResult<String> create(@RequestBody OrgCreateUpdateDto dto) {
    Org o = new Org();
    mapToEntity(dto, o);
    o.setStatus("ENABLED");
    o.setParentId(dto.getParentId() != null && !dto.getParentId().isEmpty() ? Long.parseLong(dto.getParentId()) : 0L);
    long id = orgService.create(o);
    return ApiResult.ok(String.valueOf(id));
  }

  @PutMapping("/{id}")
  public ApiResult<Void> update(@PathVariable Long id, @RequestBody OrgCreateUpdateDto dto) {
    Org o = orgService.getById(id);
    if (o == null) return ApiResult.fail(404, "组织不存在");
    mapToEntity(dto, o);
    o.setId(id);
    if (dto.getParentId() != null && !dto.getParentId().isEmpty()) {
      o.setParentId(Long.parseLong(dto.getParentId()));
    }
    orgService.update(o);
    return ApiResult.ok();
  }

  @PutMapping("/{id}/leader")
  public ApiResult<Void> updateLeader(@PathVariable Long id, @RequestBody Map<String, String> body) {
    String leaderUserId = body.get("leaderUserId");
    if (leaderUserId == null || leaderUserId.isEmpty()) return ApiResult.fail(400, "leaderUserId 必填");
    Org o = orgService.getById(id);
    if (o == null) return ApiResult.fail(404, "组织不存在");
    orgService.updateLeader(id, Long.parseLong(leaderUserId));
    return ApiResult.ok();
  }

  @PutMapping("/{id}/status")
  public ApiResult<Void> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateDto dto) {
    Org o = orgService.getById(id);
    if (o == null) return ApiResult.fail(404, "组织不存在");
    orgService.updateStatus(id, dto.getStatus());
    return ApiResult.ok();
  }

  @DeleteMapping("/{id}")
  public ApiResult<Void> delete(@PathVariable Long id) {
    Org o = orgService.getById(id);
    if (o == null) return ApiResult.fail(404, "组织不存在");
    orgService.delete(id);
    return ApiResult.ok();
  }

  private List<OrgTreeNodeDto> buildTree(List<Org> list, Long parentId) {
    return list.stream()
        .filter(o -> Objects.equals(o.getParentId(), parentId))
        .map(
            o -> {
              List<OrgTreeNodeDto> children = buildTree(list, o.getId());
              return new OrgTreeNodeDto(
                  String.valueOf(o.getId()),
                  o.getOrgCode(),
                  o.getOrgName(),
                  String.valueOf(o.getParentId()),
                  o.getLeaderUserId() != null ? String.valueOf(o.getLeaderUserId()) : null,
                  o.getLeaderNameCache(),
                  children.size(),
                  children);
            })
        .collect(Collectors.toList());
  }

  private OrgDetailDto toDetail(Org o) {
    return new OrgDetailDto(
        String.valueOf(o.getId()),
        o.getOrgCode(),
        o.getOrgName(),
        String.valueOf(o.getParentId()),
        o.getOrgType(),
        o.getOrgPath(),
        o.getLeaderUserId() != null ? String.valueOf(o.getLeaderUserId()) : null,
        o.getLeaderNameCache(),
        o.getSortOrder(),
        o.getStatus());
  }

  private void mapToEntity(OrgCreateUpdateDto dto, Org o) {
    if (dto.getTenantId() != null && !dto.getTenantId().isEmpty()) {
      o.setTenantId(Long.parseLong(dto.getTenantId()));
    }
    o.setOrgCode(dto.getOrgCode());
    o.setOrgName(dto.getOrgName());
    o.setOrgType(dto.getOrgType() != null ? dto.getOrgType() : "DEPARTMENT");
    if (dto.getLeaderUserId() != null && !dto.getLeaderUserId().isEmpty()) {
      o.setLeaderUserId(Long.parseLong(dto.getLeaderUserId()));
    }
    if (dto.getSortOrder() != null) o.setSortOrder(dto.getSortOrder());
  }
}
