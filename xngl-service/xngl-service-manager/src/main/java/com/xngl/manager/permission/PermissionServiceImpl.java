package com.xngl.manager.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.system.Permission;
import com.xngl.infrastructure.persistence.mapper.PermissionMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class PermissionServiceImpl implements PermissionService {

  private final PermissionMapper permissionMapper;

  public PermissionServiceImpl(PermissionMapper permissionMapper) {
    this.permissionMapper = permissionMapper;
  }

  @Override
  public Permission getById(Long id) {
    return permissionMapper.selectById(id);
  }

  @Override
  public IPage<Permission> page(
      Long tenantId, Long menuId, String keyword, int pageNo, int pageSize) {
    LambdaQueryWrapper<Permission> q = new LambdaQueryWrapper<>();
    if (tenantId != null) q.eq(Permission::getTenantId, tenantId);
    if (menuId != null) q.eq(Permission::getMenuId, menuId);
    if (StringUtils.hasText(keyword)) {
      q.and(
          w ->
              w.like(Permission::getPermissionCode, keyword)
                  .or()
                  .like(Permission::getPermissionName, keyword));
    }
    return permissionMapper.selectPage(new Page<>(pageNo, pageSize), q);
  }

  @Override
  public List<Permission> listByIds(List<Long> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return List.of();
    }
    return permissionMapper.selectList(new LambdaQueryWrapper<Permission>().in(Permission::getId, ids));
  }

  @Override
  public long create(Permission permission) {
    permissionMapper.insert(permission);
    return permission.getId();
  }

  @Override
  public void update(Permission permission) {
    permissionMapper.updateById(permission);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void delete(Long id) {
    permissionMapper.deleteById(id);
  }
}
