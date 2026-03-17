package com.xngl.web.controller;

import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.org.OrgService;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序出土单位/首页：当前单位、可用项目列表。
 */
@RestController
@RequestMapping("/api/mini/excavation-orgs")
public class MiniExcavationOrgsController {

  private final UserService userService;
  private final OrgService orgService;

  public MiniExcavationOrgsController(UserService userService, OrgService orgService) {
    this.userService = userService;
    this.orgService = orgService;
  }

  @GetMapping("/current")
  public ApiResult<CurrentOrgDto> current(HttpServletRequest request) {
    User user = requireCurrentUser(request);
    String orgName = "当前单位";
    if (user.getMainOrgId() != null) {
      Org org = orgService.getById(user.getMainOrgId());
      if (org != null) orgName = org.getOrgName();
    }
    return ApiResult.ok(
        new CurrentOrgDto(orgName, user.getId(), user.getName()));
  }

  @GetMapping("/projects")
  public ApiResult<ProjectListDto> projects(
      HttpServletRequest request,
      @RequestParam(required = false, defaultValue = "1") Integer pageNo,
      @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
    requireCurrentUser(request);
    // 暂无项目表分页，返回模拟数据；后续接入 biz_project 分页
    List<ProjectItemDto> records =
        List.of(
            new ProjectItemDto(1L, "滨海新区基础建设B标段", "在建"),
            new ProjectItemDto(2L, "老旧小区改造工程综合包", "在建"));
    return ApiResult.ok(new ProjectListDto(records, records.size()));
  }

  private User requireCurrentUser(HttpServletRequest request) {
    String userId = (String) request.getAttribute("userId");
    if (userId == null || userId.isBlank()) {
      throw new BizException(401, "未登录或 token 无效");
    }
    User user = userService.getById(Long.parseLong(userId));
    if (user == null) throw new BizException(401, "用户不存在");
    return user;
  }

  @lombok.Data
  public static class CurrentOrgDto {
    private String orgName;
    private Long userId;
    private String userName;

    public CurrentOrgDto(String orgName, Long userId, String userName) {
      this.orgName = orgName;
      this.userId = userId;
      this.userName = userName;
    }
  }

  @lombok.Data
  public static class ProjectListDto {
    private List<ProjectItemDto> records;
    private int total;

    public ProjectListDto(List<ProjectItemDto> records, int total) {
      this.records = records;
      this.total = total;
    }
  }

  @lombok.Data
  public static class ProjectItemDto {
    private Long id;
    private String name;
    private String status;

    public ProjectItemDto(Long id, String name, String status) {
      this.id = id;
      this.name = name;
      this.status = status;
    }
  }
}
