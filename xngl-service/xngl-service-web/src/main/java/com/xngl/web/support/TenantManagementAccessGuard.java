package com.xngl.web.support;

import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TenantManagementAccessGuard {

  private static final Set<String> TENANT_MANAGEMENT_USER_TYPES =
      Set.of("TENANT_ADMIN", "SUPER_ADMIN", "ADMIN");

  private final UserContext userContext;

  public TenantManagementAccessGuard(UserContext userContext) {
    this.userContext = userContext;
  }

  public User requireTenantManagementUser(HttpServletRequest request) {
    User user = userContext.requireCurrentUser(request);
    if (!TENANT_MANAGEMENT_USER_TYPES.contains(normalize(user.getUserType()))) {
      throw new BizException(403, "当前用户无权操作租户级权限配置");
    }
    return user;
  }

  public void ensureSameTenant(Long resourceTenantId, Long currentTenantId, String resourceName) {
    if (resourceTenantId == null || currentTenantId == null || !Objects.equals(resourceTenantId, currentTenantId)) {
      throw new BizException(404, resourceName + "不存在");
    }
  }

  private String normalize(String userType) {
    return userType == null ? "" : userType.trim().toUpperCase(Locale.ROOT);
  }
}
