package com.xngl.manager.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.Role;
import com.xngl.infrastructure.persistence.entity.organization.Tenant;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.RoleMapper;
import com.xngl.infrastructure.persistence.mapper.TenantMapper;
import com.xngl.infrastructure.persistence.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TenantServiceImpl implements TenantService {

  private final TenantMapper tenantMapper;
  private final OrgMapper orgMapper;
  private final UserMapper userMapper;
  private final RoleMapper roleMapper;

  public TenantServiceImpl(
      TenantMapper tenantMapper,
      OrgMapper orgMapper,
      UserMapper userMapper,
      RoleMapper roleMapper) {
    this.tenantMapper = tenantMapper;
    this.orgMapper = orgMapper;
    this.userMapper = userMapper;
    this.roleMapper = roleMapper;
  }

  @Override
  public Tenant getById(Long id) {
    return tenantMapper.selectById(id);
  }

  @Override
  public IPage<Tenant> page(
      String tenantName, String tenantType, String status, int pageNo, int pageSize) {
    LambdaQueryWrapper<Tenant> q = new LambdaQueryWrapper<>();
    if (StringUtils.hasText(tenantName)) q.like(Tenant::getTenantName, tenantName);
    if (StringUtils.hasText(tenantType)) q.eq(Tenant::getTenantType, tenantType);
    if (StringUtils.hasText(status)) q.eq(Tenant::getStatus, status);
    return tenantMapper.selectPage(new Page<>(pageNo, pageSize), q);
  }

  @Override
  public TenantSummaryVo getSummary(Long tenantId) {
    Tenant t = tenantMapper.selectById(tenantId);
    if (t == null) return null;
    long orgCount =
        orgMapper.selectCount(new LambdaQueryWrapper<Org>().eq(Org::getTenantId, tenantId));
    long userCount =
        userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getTenantId, tenantId));
    long roleCount =
        roleMapper.selectCount(new LambdaQueryWrapper<Role>().eq(Role::getTenantId, tenantId));
    return new TenantSummaryVo(tenantId, orgCount, userCount, roleCount, t.getStatus());
  }

  @Override
  public long create(Tenant tenant) {
    tenantMapper.insert(tenant);
    return tenant.getId();
  }

  @Override
  public void update(Tenant tenant) {
    tenantMapper.updateById(tenant);
  }

  @Override
  public void updateStatus(Long id, String status) {
    Tenant t = new Tenant();
    t.setId(id);
    t.setStatus(status);
    tenantMapper.updateById(t);
  }
}
