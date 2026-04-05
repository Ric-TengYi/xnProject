package com.xngl.web.support;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class MasterDataAccessScope {

  private final boolean tenantWideAccess;
  private final Set<Long> orgIds;
  private final Set<Long> projectIds;

  private MasterDataAccessScope(
      boolean tenantWideAccess, Collection<Long> orgIds, Collection<Long> projectIds) {
    this.tenantWideAccess = tenantWideAccess;
    this.orgIds = immutableCopy(orgIds);
    this.projectIds = immutableCopy(projectIds);
  }

  public static MasterDataAccessScope tenantWide(Collection<Long> orgIds) {
    return new MasterDataAccessScope(true, orgIds, Set.of());
  }

  public static MasterDataAccessScope scoped(
      Collection<Long> orgIds, Collection<Long> projectIds) {
    return new MasterDataAccessScope(false, orgIds, projectIds);
  }

  public static MasterDataAccessScope none() {
    return new MasterDataAccessScope(false, Set.of(), Set.of());
  }

  public boolean isTenantWideAccess() {
    return tenantWideAccess;
  }

  public Set<Long> getOrgIds() {
    return orgIds;
  }

  public Set<Long> getProjectIds() {
    return projectIds;
  }

  public boolean hasAnyAccess() {
    return tenantWideAccess || !orgIds.isEmpty() || !projectIds.isEmpty();
  }

  public boolean hasOrgAccess(Long orgId) {
    return orgId != null && orgIds.contains(orgId);
  }

  public boolean hasProjectAccess(Long projectId) {
    return projectId != null && projectIds.contains(projectId);
  }

  private static Set<Long> immutableCopy(Collection<Long> values) {
    LinkedHashSet<Long> result = new LinkedHashSet<>();
    if (values != null) {
      for (Long value : values) {
        if (value != null) {
          result.add(value);
        }
      }
    }
    return Set.copyOf(result);
  }
}
