package com.xngl.manager.org;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.Role;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.manager.role.RoleService;
import com.xngl.manager.user.UserService;
import java.security.SecureRandom;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OrgServiceImpl implements OrgService {

  private static final String ADMIN_SUFFIX = "_admin";
  private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
  private static final int PASSWORD_LENGTH = 8;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final OrgMapper orgMapper;
  private final UserService userService;
  private final RoleService roleService;

  public OrgServiceImpl(OrgMapper orgMapper, UserService userService, RoleService roleService) {
    this.orgMapper = orgMapper;
    this.userService = userService;
    this.roleService = roleService;
  }

  @Override
  public Org getById(Long id) {
    return orgMapper.selectById(id);
  }

  @Override
  public List<Org> listTree(Long tenantId, String keyword, String status) {
    if (tenantId == null) return List.of();
    LambdaQueryWrapper<Org> q = new LambdaQueryWrapper<>();
    q.eq(Org::getTenantId, tenantId);
    if (StringUtils.hasText(keyword)) {
      q.and(
          w ->
              w.like(Org::getOrgCode, keyword)
                  .or()
                  .like(Org::getOrgName, keyword));
    }
    if (StringUtils.hasText(status)) q.eq(Org::getStatus, status);
    q.orderByAsc(Org::getSortOrder);
    return orgMapper.selectList(q);
  }

  @Override
  public List<Org> listByTenantId(Long tenantId) {
    if (tenantId == null) return List.of();
    return orgMapper.selectList(
        new LambdaQueryWrapper<Org>()
            .eq(Org::getTenantId, tenantId)
            .orderByAsc(Org::getSortOrder));
  }

  @Override
  public long create(Org org) {
    orgMapper.insert(org);
    return org.getId();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public CreateOrgResult createWithAdmin(Long tenantId, Long operatorId, Org org) {
    // 1. 创建组织
    org.setTenantId(tenantId);
    if (org.getParentId() == null) {
      org.setParentId(0L);
    }
    if (!StringUtils.hasText(org.getOrgPath())) {
      org.setOrgPath("/" + org.getParentId());
    }
    if (!StringUtils.hasText(org.getStatus())) {
      org.setStatus("ENABLED");
    }
    orgMapper.insert(org);
    long orgId = org.getId();

    // 更新 orgPath 包含自身 id
    org.setOrgPath(org.getOrgPath() + "/" + orgId);
    orgMapper.updateById(org);

    // 2. 生成管理员账号
    String orgCode = StringUtils.hasText(org.getOrgCode()) ? org.getOrgCode() : "org" + orgId;
    String adminUsername = orgCode.toLowerCase().replaceAll("[^a-z0-9_]", "") + ADMIN_SUFFIX;
    String plainPassword = generateRandomPassword();

    User admin = new User();
    admin.setTenantId(tenantId);
    admin.setUsername(adminUsername);
    admin.setName(org.getOrgName() + "管理员");
    admin.setPasswordEncrypted(plainPassword);
    admin.setUserType("ORG_ADMIN");
    admin.setMainOrgId(orgId);
    admin.setStatus("ENABLED");
    admin.setLockStatus(0);
    admin.setAuthSource("LOCAL");
    admin.setNeedResetPassword(1);
    long adminUserId = userService.create(admin);

    // 3. 绑定组织关系
    userService.updateOrgs(adminUserId, orgId, List.of(orgId));

    // 4. 绑定 ORG_ADMIN 角色
    List<Role> orgAdminRoles = roleService.listByRoleCode(tenantId, "ORG_ADMIN");
    if (!orgAdminRoles.isEmpty()) {
      userService.updateRoles(adminUserId, orgAdminRoles.stream().map(Role::getId).toList());
    }

    // 5. 回填组织负责人
    org.setLeaderUserId(adminUserId);
    org.setLeaderNameCache(admin.getName());
    orgMapper.updateById(org);

    return new CreateOrgResult(orgId, adminUserId, adminUsername, plainPassword);
  }

  @Override
  public void update(Org org) {
    orgMapper.updateById(org);
  }

  @Override
  public void updateLeader(Long id, Long leaderUserId) {
    Org o = new Org();
    o.setId(id);
    o.setLeaderUserId(leaderUserId);
    orgMapper.updateById(o);
  }

  @Override
  public void updateStatus(Long id, String status) {
    Org o = new Org();
    o.setId(id);
    o.setStatus(status);
    orgMapper.updateById(o);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void delete(Long id) {
    orgMapper.deleteById(id);
  }

  private String generateRandomPassword() {
    StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
    for (int i = 0; i < PASSWORD_LENGTH; i++) {
      sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
    }
    return sb.toString();
  }
}
