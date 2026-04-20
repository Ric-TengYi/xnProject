package com.xngl.web.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.infrastructure.persistence.mapper.UserMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.manager.contract.ContractAccessScope;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CollaborationAccessScopeResolver {

  private final MasterDataAccessScopeResolver masterDataAccessScopeResolver;
  private final ContractAccessScopeResolver contractAccessScopeResolver;
  private final ProjectMapper projectMapper;
  private final SiteMapper siteMapper;
  private final VehicleMapper vehicleMapper;
  private final ContractMapper contractMapper;
  private final UserMapper userMapper;

  public CollaborationAccessScopeResolver(
      MasterDataAccessScopeResolver masterDataAccessScopeResolver,
      ContractAccessScopeResolver contractAccessScopeResolver,
      ProjectMapper projectMapper,
      SiteMapper siteMapper,
      VehicleMapper vehicleMapper,
      ContractMapper contractMapper,
      UserMapper userMapper) {
    this.masterDataAccessScopeResolver = masterDataAccessScopeResolver;
    this.contractAccessScopeResolver = contractAccessScopeResolver;
    this.projectMapper = projectMapper;
    this.siteMapper = siteMapper;
    this.vehicleMapper = vehicleMapper;
    this.contractMapper = contractMapper;
    this.userMapper = userMapper;
  }

  public CollaborationAccessScope resolve(User currentUser) {
    Long currentUserId = currentUser != null ? currentUser.getId() : null;
    if (currentUser == null || currentUser.getTenantId() == null) {
      return CollaborationAccessScope.none(currentUserId);
    }

    MasterDataAccessScope projectScope =
        masterDataAccessScopeResolver.resolve(
            currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_PROJECT);
    MasterDataAccessScope siteScope =
        masterDataAccessScopeResolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_SITE);
    MasterDataAccessScope vehicleScope =
        masterDataAccessScopeResolver.resolve(
            currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_VEHICLE);
    ContractAccessScope contractScope = contractAccessScopeResolver.resolve(currentUser);

    if (projectScope.isTenantWideAccess()
        || siteScope.isTenantWideAccess()
        || vehicleScope.isTenantWideAccess()
        || contractScope.isTenantWideAccess()) {
      return CollaborationAccessScope.tenantWide(currentUserId);
    }

    LinkedHashSet<Long> orgIds = new LinkedHashSet<>();
    LinkedHashSet<Long> projectIds = new LinkedHashSet<>();
    LinkedHashSet<Long> siteIds = new LinkedHashSet<>();
    LinkedHashSet<Long> vehicleIds = new LinkedHashSet<>();
    LinkedHashSet<Long> contractIds = new LinkedHashSet<>();
    LinkedHashSet<Long> userIds = new LinkedHashSet<>();

    unionScope(orgIds, projectIds, projectScope);
    unionScope(orgIds, projectIds, siteScope);
    unionScope(orgIds, projectIds, vehicleScope);
    orgIds.addAll(contractScope.getOrgIds());
    projectIds.addAll(contractScope.getProjectIds());
    siteIds.addAll(contractScope.getSiteIds());

    expandProjects(orgIds, projectIds);
    expandSites(orgIds, projectIds, siteIds);
    expandVehicles(orgIds, vehicleIds, currentUser.getTenantId());
    expandContracts(orgIds, projectIds, siteIds, contractIds, currentUser.getTenantId());
    expandUsers(orgIds, userIds, currentUser.getTenantId(), currentUserId);

    return CollaborationAccessScope.scoped(
        currentUserId, orgIds, projectIds, siteIds, vehicleIds, contractIds, userIds);
  }

  private void unionScope(
      Set<Long> orgIds, Set<Long> projectIds, MasterDataAccessScope masterDataAccessScope) {
    orgIds.addAll(masterDataAccessScope.getOrgIds());
    projectIds.addAll(masterDataAccessScope.getProjectIds());
  }

  private void expandProjects(Set<Long> orgIds, Set<Long> projectIds) {
    if (orgIds.isEmpty() && projectIds.isEmpty()) {
      return;
    }
    List<Project> projects =
        projectMapper.selectList(
            new LambdaQueryWrapper<Project>()
                .and(
                    wrapper -> {
                      boolean hasCondition = false;
                      if (!orgIds.isEmpty()) {
                        wrapper.in(Project::getOrgId, orgIds);
                        hasCondition = true;
                      }
                      if (!projectIds.isEmpty()) {
                        if (hasCondition) {
                          wrapper.or();
                        }
                        wrapper.in(Project::getId, projectIds);
                      }
                    }));
    for (Project project : projects) {
      if (project.getId() != null) {
        projectIds.add(project.getId());
      }
      if (project.getOrgId() != null) {
        orgIds.add(project.getOrgId());
      }
    }
  }

  private void expandSites(Set<Long> orgIds, Set<Long> projectIds, Set<Long> siteIds) {
    if (orgIds.isEmpty() && projectIds.isEmpty() && siteIds.isEmpty()) {
      return;
    }
    List<Site> sites =
        siteMapper.selectList(
            new LambdaQueryWrapper<Site>()
                .and(
                    wrapper -> {
                      boolean hasCondition = false;
                      if (!orgIds.isEmpty()) {
                        wrapper.in(Site::getOrgId, orgIds);
                        hasCondition = true;
                      }
                      if (!projectIds.isEmpty()) {
                        if (hasCondition) {
                          wrapper.or();
                        }
                        wrapper.in(Site::getProjectId, projectIds);
                        hasCondition = true;
                      }
                      if (!siteIds.isEmpty()) {
                        if (hasCondition) {
                          wrapper.or();
                        }
                        wrapper.in(Site::getId, siteIds);
                      }
                    }));
    for (Site site : sites) {
      if (site.getId() != null) {
        siteIds.add(site.getId());
      }
      if (site.getProjectId() != null) {
        projectIds.add(site.getProjectId());
      }
      if (site.getOrgId() != null) {
        orgIds.add(site.getOrgId());
      }
    }
  }

  private void expandVehicles(Set<Long> orgIds, Set<Long> vehicleIds, Long tenantId) {
    if (tenantId == null || orgIds.isEmpty()) {
      return;
    }
    List<Vehicle> vehicles =
        vehicleMapper.selectList(
            new LambdaQueryWrapper<Vehicle>()
                .eq(Vehicle::getTenantId, tenantId)
                .in(Vehicle::getOrgId, orgIds));
    for (Vehicle vehicle : vehicles) {
      if (vehicle.getId() != null) {
        vehicleIds.add(vehicle.getId());
      }
      if (vehicle.getOrgId() != null) {
        orgIds.add(vehicle.getOrgId());
      }
    }
  }

  private void expandContracts(
      Set<Long> orgIds,
      Set<Long> projectIds,
      Set<Long> siteIds,
      Set<Long> contractIds,
      Long tenantId) {
    if (tenantId == null || (orgIds.isEmpty() && projectIds.isEmpty() && siteIds.isEmpty())) {
      return;
    }
    List<Contract> contracts =
        contractMapper.selectList(
            new LambdaQueryWrapper<Contract>()
                .eq(Contract::getTenantId, tenantId)
                .and(
                    wrapper -> {
                      boolean hasCondition = false;
                      if (!projectIds.isEmpty()) {
                        wrapper.in(Contract::getProjectId, projectIds);
                        hasCondition = true;
                      }
                      if (!siteIds.isEmpty()) {
                        if (hasCondition) {
                          wrapper.or();
                        }
                        wrapper.in(Contract::getSiteId, siteIds);
                        hasCondition = true;
                      }
                      if (!orgIds.isEmpty()) {
                        if (hasCondition) {
                          wrapper.or();
                        }
                        wrapper.in(Contract::getConstructionOrgId, orgIds)
                            .or()
                            .in(Contract::getTransportOrgId, orgIds)
                            .or()
                            .in(Contract::getSiteOperatorOrgId, orgIds)
                            .or()
                            .in(Contract::getPartyId, orgIds);
                      }
                    }));
    for (Contract contract : contracts) {
      if (contract.getId() != null) {
        contractIds.add(contract.getId());
      }
      if (contract.getProjectId() != null) {
        projectIds.add(contract.getProjectId());
      }
      if (contract.getSiteId() != null) {
        siteIds.add(contract.getSiteId());
      }
      addIfNotNull(orgIds, contract.getConstructionOrgId());
      addIfNotNull(orgIds, contract.getTransportOrgId());
      addIfNotNull(orgIds, contract.getSiteOperatorOrgId());
      addIfNotNull(orgIds, contract.getPartyId());
    }
  }

  private void expandUsers(
      Set<Long> orgIds, Set<Long> userIds, Long tenantId, Long currentUserId) {
    if (currentUserId != null) {
      userIds.add(currentUserId);
    }
    if (tenantId == null || orgIds.isEmpty()) {
      return;
    }
    List<User> users =
        userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .eq(User::getTenantId, tenantId)
                .in(User::getMainOrgId, orgIds));
    for (User user : users) {
      if (user.getId() != null) {
        userIds.add(user.getId());
      }
      addIfNotNull(orgIds, user.getMainOrgId());
    }
  }

  private void addIfNotNull(Set<Long> ids, Long value) {
    if (value != null) {
      ids.add(value);
    }
  }
}
