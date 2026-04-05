package com.xngl.web.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.manager.contract.ContractAccessScope;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ContractAccessScopeResolver {

  private final MasterDataAccessScopeResolver masterDataAccessScopeResolver;
  private final OrgMapper orgMapper;
  private final ProjectMapper projectMapper;
  private final SiteMapper siteMapper;

  public ContractAccessScopeResolver(
      MasterDataAccessScopeResolver masterDataAccessScopeResolver,
      OrgMapper orgMapper,
      ProjectMapper projectMapper,
      SiteMapper siteMapper) {
    this.masterDataAccessScopeResolver = masterDataAccessScopeResolver;
    this.orgMapper = orgMapper;
    this.projectMapper = projectMapper;
    this.siteMapper = siteMapper;
  }

  public ContractAccessScope resolve(User currentUser) {
    if (currentUser == null || currentUser.getTenantId() == null) {
      return ContractAccessScope.scoped(Set.of(), Set.of(), Set.of());
    }
    MasterDataAccessScope baseScope =
        masterDataAccessScopeResolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_CONTRACT);
    if (baseScope.isTenantWideAccess()) {
      return ContractAccessScope.tenantWide();
    }

    LinkedHashSet<Long> tenantOrgIds =
        orgMapper.selectList(
                new LambdaQueryWrapper<Org>().eq(Org::getTenantId, currentUser.getTenantId()))
            .stream()
            .map(Org::getId)
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    LinkedHashSet<Long> orgIds = new LinkedHashSet<>(baseScope.getOrgIds());
    orgIds.retainAll(tenantOrgIds);

    LinkedHashSet<Long> projectIds = new LinkedHashSet<>(baseScope.getProjectIds());
    if (!orgIds.isEmpty() || !projectIds.isEmpty()) {
      List<Project> projects =
          projectMapper.selectList(
              new LambdaQueryWrapper<Project>()
                  .and(
                      wrapper -> {
                        boolean hasClause = false;
                        if (!orgIds.isEmpty()) {
                          wrapper.in(Project::getOrgId, orgIds);
                          hasClause = true;
                        }
                        if (!projectIds.isEmpty()) {
                          if (hasClause) {
                            wrapper.or();
                          }
                          wrapper.in(Project::getId, projectIds);
                        }
                      }));
      for (Project project : projects) {
        if (project.getId() != null) {
          projectIds.add(project.getId());
        }
        if (project.getOrgId() != null && tenantOrgIds.contains(project.getOrgId())) {
          orgIds.add(project.getOrgId());
        }
      }
    }

    LinkedHashSet<Long> siteIds = new LinkedHashSet<>();
    if (!orgIds.isEmpty() || !projectIds.isEmpty()) {
      List<Site> sites =
          siteMapper.selectList(
              new LambdaQueryWrapper<Site>()
                  .and(
                      wrapper -> {
                        boolean hasClause = false;
                        if (!orgIds.isEmpty()) {
                          wrapper.in(Site::getOrgId, orgIds);
                          hasClause = true;
                        }
                        if (!projectIds.isEmpty()) {
                          if (hasClause) {
                            wrapper.or();
                          }
                          wrapper.in(Site::getProjectId, projectIds);
                        }
                      }));
      for (Site site : sites) {
        if (site.getId() != null) {
          siteIds.add(site.getId());
        }
        if (site.getProjectId() != null) {
          projectIds.add(site.getProjectId());
        }
        if (site.getOrgId() != null && tenantOrgIds.contains(site.getOrgId())) {
          orgIds.add(site.getOrgId());
        }
      }
    }

    return ContractAccessScope.scoped(orgIds, projectIds, siteIds);
  }
}
