package com.xngl.manager.contract;

import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.SettlementOrder;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ContractAccessScope {

  private final boolean tenantWideAccess;
  private final Set<Long> orgIds;
  private final Set<Long> projectIds;
  private final Set<Long> siteIds;

  private ContractAccessScope(
      boolean tenantWideAccess,
      Collection<Long> orgIds,
      Collection<Long> projectIds,
      Collection<Long> siteIds) {
    this.tenantWideAccess = tenantWideAccess;
    this.orgIds = immutableCopy(orgIds);
    this.projectIds = immutableCopy(projectIds);
    this.siteIds = immutableCopy(siteIds);
  }

  public static ContractAccessScope tenantWide() {
    return new ContractAccessScope(true, Set.of(), Set.of(), Set.of());
  }

  public static ContractAccessScope scoped(
      Collection<Long> orgIds, Collection<Long> projectIds, Collection<Long> siteIds) {
    return new ContractAccessScope(false, orgIds, projectIds, siteIds);
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

  public Set<Long> getSiteIds() {
    return siteIds;
  }

  public boolean hasAnyAccess() {
    return tenantWideAccess || !orgIds.isEmpty() || !projectIds.isEmpty() || !siteIds.isEmpty();
  }

  public boolean hasProjectAccess(Long projectId) {
    return tenantWideAccess || (projectId != null && projectIds.contains(projectId));
  }

  public boolean hasSiteAccess(Long siteId) {
    return tenantWideAccess || (siteId != null && siteIds.contains(siteId));
  }

  public boolean matchesContract(Contract contract) {
    if (tenantWideAccess) {
      return true;
    }
    if (contract == null) {
      return false;
    }
    return matchesAny(
        contract.getProjectId(),
        contract.getSiteId(),
        contract.getConstructionOrgId(),
        contract.getTransportOrgId(),
        contract.getSiteOperatorOrgId(),
        contract.getPartyId());
  }

  public boolean matchesSettlement(SettlementOrder order) {
    if (tenantWideAccess) {
      return true;
    }
    if (order == null) {
      return false;
    }
    return hasProjectAccess(order.getTargetProjectId()) || hasSiteAccess(order.getTargetSiteId());
  }

  private boolean matchesAny(
      Long projectId,
      Long siteId,
      Long constructionOrgId,
      Long transportOrgId,
      Long siteOperatorOrgId,
      Long partyId) {
    return hasProjectAccess(projectId)
        || hasSiteAccess(siteId)
        || hasOrgAccess(constructionOrgId)
        || hasOrgAccess(transportOrgId)
        || hasOrgAccess(siteOperatorOrgId)
        || hasOrgAccess(partyId);
  }

  private boolean hasOrgAccess(Long orgId) {
    return tenantWideAccess || (orgId != null && orgIds.contains(orgId));
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
