package com.xngl.web.support;

import com.xngl.infrastructure.persistence.entity.organization.Role;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.role.RoleService;
import com.xngl.manager.user.UserService;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class UserContext {

  private static final Set<String> APPROVAL_ROLE_CODES =
      Set.of("ADMIN", "SUPER_ADMIN", "APPROVAL_MANAGER", "CONTRACT_APPROVER", "SETTLEMENT_APPROVER");

  private final UserService userService;
  private final RoleService roleService;

  public UserContext(UserService userService, RoleService roleService) {
    this.userService = userService;
    this.roleService = roleService;
  }

  public User requireCurrentUser(HttpServletRequest request) {
    String userId = (String) request.getAttribute("userId");
    if (userId == null || userId.isBlank()) {
      throw new BizException(401, "未登录或 token 无效");
    }
    try {
      User user = userService.getById(Long.parseLong(userId));
      if (user == null) {
        throw new BizException(401, "用户不存在");
      }
      if (user.getTenantId() == null) {
        throw new BizException(403, "当前用户未绑定租户");
      }
      return user;
    } catch (NumberFormatException ex) {
      throw new BizException(401, "token 中的用户信息无效");
    }
  }

  public void requireApprovalPermission(User user) {
    if (user == null || user.getId() == null) {
      throw new BizException(403, "无审批权限");
    }
    List<Long> roleIds = userService.listRoleIdsByUserId(user.getId());
    if (roleIds == null || roleIds.isEmpty()) {
      throw new BizException(403, "当前用户无审批权限，请联系管理员分配角色");
    }
    List<Role> roles = roleService.listByIds(roleIds);
    boolean hasApprovalRole = roles.stream()
        .anyMatch(role -> role.getRoleCode() != null
            && APPROVAL_ROLE_CODES.contains(role.getRoleCode().toUpperCase()));
    if (!hasApprovalRole) {
      throw new BizException(403, "当前用户无审批权限，请联系管理员分配审批角色");
    }
  }
}
