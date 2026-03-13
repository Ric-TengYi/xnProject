package com.xngl.manager.auth;

import com.xngl.infrastructure.persistence.entity.organization.User;

public interface AuthService {

  User getByUsername(String username);

  User getByTenantAndUsername(Long tenantId, String username);

  boolean checkPassword(User user, String rawPassword);
}
