package com.xngl.web.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.Role;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.system.DataScopeRule;
import com.xngl.infrastructure.persistence.mapper.DataScopeRuleMapper;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.manager.role.RoleService;
import com.xngl.manager.user.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MasterDataAccessScopeResolverTest {

  @Mock
  private UserService userService;

  @Mock
  private RoleService roleService;

  @Mock
  private OrgMapper orgMapper;

  @Mock
  private DataScopeRuleMapper dataScopeRuleMapper;

  @Test
  void resolveShouldReturnTenantWideScopeForTenantAdmin() {
    MasterDataAccessScopeResolver resolver =
        new MasterDataAccessScopeResolver(userService, roleService, orgMapper, dataScopeRuleMapper);
    User currentUser = buildUser(7L, 1L, "TENANT_ADMIN", 10L);

    when(orgMapper.selectList(org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(buildOrg(10L, 1L, "/0/10"), buildOrg(11L, 1L, "/0/10/11")));

    MasterDataAccessScope scope =
        resolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_PROJECT);

    assertThat(scope.isTenantWideAccess()).isTrue();
    assertThat(scope.getOrgIds()).containsExactlyInAnyOrder(10L, 11L);
    assertThat(scope.getProjectIds()).isEmpty();
  }

  @Test
  void resolveShouldExpandAssignedOrgChildrenForOrgAndChildrenScope() {
    MasterDataAccessScopeResolver resolver =
        new MasterDataAccessScopeResolver(userService, roleService, orgMapper, dataScopeRuleMapper);
    User currentUser = buildUser(8L, 1L, "ORG_ADMIN", 10L);
    Role orgRole = new Role();
    orgRole.setId(100L);
    orgRole.setTenantId(1L);
    orgRole.setRoleScope("ORG");
    orgRole.setDataScopeTypeDefault("ORG_AND_CHILDREN");

    when(userService.listRoleIdsByUserId(8L)).thenReturn(List.of(100L));
    when(userService.listOrgIdsByUserId(8L)).thenReturn(List.of(10L));
    when(roleService.listByIds(List.of(100L))).thenReturn(List.of(orgRole));
    when(dataScopeRuleMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
    when(orgMapper.selectList(org.mockito.ArgumentMatchers.any()))
        .thenReturn(
            List.of(
                buildOrg(10L, 1L, "/0/10"),
                buildOrg(11L, 1L, "/0/10/11"),
                buildOrg(12L, 1L, "/0/12")));

    MasterDataAccessScope scope =
        resolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_VEHICLE);

    assertThat(scope.isTenantWideAccess()).isFalse();
    assertThat(scope.getOrgIds()).containsExactlyInAnyOrder(10L, 11L);
    assertThat(scope.getProjectIds()).isEmpty();
  }

  @Test
  void resolveShouldReadCustomOrgAndProjectRules() {
    MasterDataAccessScopeResolver resolver =
        new MasterDataAccessScopeResolver(userService, roleService, orgMapper, dataScopeRuleMapper);
    User currentUser = buildUser(9L, 1L, "USER", 10L);
    Role scopedRole = new Role();
    scopedRole.setId(101L);
    scopedRole.setTenantId(1L);
    scopedRole.setRoleScope("ORG");
    scopedRole.setDataScopeTypeDefault("SELF");

    DataScopeRule orgRule = new DataScopeRule();
    orgRule.setRoleId(101L);
    orgRule.setTenantId(1L);
    orgRule.setBizModule(MasterDataAccessScopeResolver.BIZ_MODULE_PROJECT);
    orgRule.setScopeType("CUSTOM_ORG_SET");
    orgRule.setScopeValue("[12]");

    DataScopeRule projectRule = new DataScopeRule();
    projectRule.setRoleId(101L);
    projectRule.setTenantId(1L);
    projectRule.setBizModule(MasterDataAccessScopeResolver.BIZ_MODULE_PROJECT);
    projectRule.setScopeType("CUSTOM_PROJECT_SET");
    projectRule.setScopeValue("[100,101]");

    when(userService.listRoleIdsByUserId(9L)).thenReturn(List.of(101L));
    when(userService.listOrgIdsByUserId(9L)).thenReturn(List.of(10L));
    when(roleService.listByIds(List.of(101L))).thenReturn(List.of(scopedRole));
    when(dataScopeRuleMapper.selectList(org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(orgRule, projectRule));
    when(orgMapper.selectList(org.mockito.ArgumentMatchers.any()))
        .thenReturn(
            List.of(
                buildOrg(10L, 1L, "/0/10"),
                buildOrg(11L, 1L, "/0/10/11"),
                buildOrg(12L, 1L, "/0/12")));

    MasterDataAccessScope scope =
        resolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_PROJECT);

    assertThat(scope.isTenantWideAccess()).isFalse();
    assertThat(scope.getOrgIds()).containsExactly(12L);
    assertThat(scope.getProjectIds()).containsExactlyInAnyOrder(100L, 101L);
  }

  private User buildUser(Long id, Long tenantId, String userType, Long mainOrgId) {
    User user = new User();
    user.setId(id);
    user.setTenantId(tenantId);
    user.setUserType(userType);
    user.setMainOrgId(mainOrgId);
    return user;
  }

  private Org buildOrg(Long id, Long tenantId, String orgPath) {
    Org org = new Org();
    org.setId(id);
    org.setTenantId(tenantId);
    org.setOrgPath(orgPath);
    return org;
  }
}
