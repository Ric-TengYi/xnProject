package com.xngl.manager.permission;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.system.Permission;

public interface PermissionService {

  Permission getById(Long id);

  IPage<Permission> page(
      Long tenantId, Long menuId, String keyword, int pageNo, int pageSize);

  java.util.List<Permission> listByIds(java.util.List<Long> ids);

  long create(Permission permission);

  void update(Permission permission);

  void delete(Long id);
}
