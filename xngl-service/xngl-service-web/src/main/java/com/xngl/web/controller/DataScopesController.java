package com.xngl.web.controller;

import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.user.DataScopePreviewDto;
import com.xngl.web.dto.user.DataScopeTemplateDto;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * 数据权限模板与预览。契约见 02-api-dto-contract 第 9 节。
 * 当前为 stub：模板返回固定枚举，预览返回空范围。
 */
@RestController
@RequestMapping("/api/data-scopes")
public class DataScopesController {

  @GetMapping("/templates")
  public ApiResult<List<DataScopeTemplateDto>> templates() {
    List<DataScopeTemplateDto> list =
        List.of(
            new DataScopeTemplateDto("ALL", "全部数据", false, false),
            new DataScopeTemplateDto("TENANT", "本租户", false, false),
            new DataScopeTemplateDto("ORG_AND_CHILDREN", "本组织及子组织", true, false),
            new DataScopeTemplateDto("SELF", "仅本人", false, false),
            new DataScopeTemplateDto("CUSTOM_ORG_SET", "自定义组织", true, false),
            new DataScopeTemplateDto("CUSTOM_PROJECT_SET", "自定义项目", false, true));
    return ApiResult.ok(list);
  }

  @PostMapping("/preview")
  public ApiResult<DataScopePreviewDto> preview(@RequestBody DataScopePreviewRequest req) {
    DataScopePreviewDto dto =
        new DataScopePreviewDto(List.of(), List.of(), "当前为预览 stub，未解析实际范围");
    return ApiResult.ok(dto);
  }

  @lombok.Data
  public static class DataScopePreviewRequest {
    private String tenantId;
    private java.util.List<String> roleIds;
    private java.util.List<RoleDataScopeItem> scopeRules;
  }

  @lombok.Data
  public static class RoleDataScopeItem {
    private String bizModule;
    private String scopeType;
    private java.util.List<String> orgIds;
    private java.util.List<String> projectIds;
  }
}
