package com.xngl.manager.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.organization.User;

import java.util.List;

public interface UserService {

  User getById(Long id);

  IPage<User> page(
      Long tenantId, String keyword, Long orgId, String status, int pageNo, int pageSize);

  long create(User user);

  void update(User user);

  /** 仅更新最后登录时间，避免全量 update 将其他字段置空 */
  void updateLastLoginTime(Long userId);

  void updateStatus(Long id, String status);

  void delete(Long id);

  void resetPassword(Long id, String newPasswordEncrypted);

  User getByUsername(String username);

  List<Long> listRoleIdsByUserId(Long userId);

  void updateRoles(Long userId, List<Long> roleIds);

  List<Long> listOrgIdsByUserId(Long userId);

  Long getMainOrgIdByUserId(Long userId);

  void updateOrgs(Long userId, Long mainOrgId, List<Long> orgIds);
}
