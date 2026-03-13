package com.xngl.web.controller;

import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.manager.site.SiteService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.site.DisposalListItemDto;
import com.xngl.web.dto.site.SiteDetailDto;
import com.xngl.web.dto.site.SiteListItemDto;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sites")
public class SitesController {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final SiteService siteService;

  public SitesController(SiteService siteService) {
    this.siteService = siteService;
  }

  @GetMapping
  public ApiResult<List<SiteListItemDto>> list() {
    List<Site> sites = siteService.list();
    List<SiteListItemDto> records =
        sites.stream().map(this::toListItem).collect(Collectors.toList());
    return ApiResult.ok(records);
  }

  @GetMapping("/{id}")
  public ApiResult<SiteDetailDto> get(@PathVariable Long id) {
    Site site = siteService.getById(id);
    if (site == null) return ApiResult.fail(404, "场地不存在");
    return ApiResult.ok(toDetail(site));
  }

  /**
   * 消纳清单分页查询。当前无 biz_disposal 表，返回空分页便于前端联调；
   * 建表后在此接入 DisposalService 即可。
   */
  @GetMapping("/disposals")
  public ApiResult<PageResult<DisposalListItemDto>> listDisposals(
      @RequestParam(required = false) Long siteId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize) {
    PageResult<DisposalListItemDto> empty =
        new PageResult<>((long) pageNo, (long) pageSize, 0L, Collections.emptyList());
    return ApiResult.ok(empty);
  }

  @PostMapping
  public ApiResult<?> create(@RequestBody Object body) {
    return ApiResult.ok(null);
  }

  private SiteListItemDto toListItem(Site s) {
    SiteListItemDto dto = new SiteListItemDto();
    dto.setId(s.getId() != null ? String.valueOf(s.getId()) : null);
    dto.setName(s.getName());
    dto.setCode(s.getCode());
    dto.setAddress(s.getAddress());
    dto.setStatus(s.getStatus());
    return dto;
  }

  private SiteDetailDto toDetail(Site s) {
    SiteDetailDto dto = new SiteDetailDto();
    dto.setId(s.getId() != null ? String.valueOf(s.getId()) : null);
    dto.setName(s.getName());
    dto.setCode(s.getCode());
    dto.setAddress(s.getAddress());
    dto.setProjectId(s.getProjectId());
    dto.setStatus(s.getStatus());
    dto.setOrgId(s.getOrgId());
    dto.setCreateTime(s.getCreateTime() != null ? s.getCreateTime().format(ISO) : null);
    dto.setUpdateTime(s.getUpdateTime() != null ? s.getUpdateTime().format(ISO) : null);
    return dto;
  }
}
