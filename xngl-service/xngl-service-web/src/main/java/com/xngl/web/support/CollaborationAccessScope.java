package com.xngl.web.support;

import com.xngl.infrastructure.persistence.entity.alert.AlertEvent;
import com.xngl.infrastructure.persistence.entity.event.ManualEvent;
import com.xngl.infrastructure.persistence.entity.security.SecurityInspection;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class CollaborationAccessScope {

  private final boolean tenantWideAccess;
  private final Long currentUserId;
  private final Set<Long> orgIds;
  private final Set<Long> projectIds;
  private final Set<Long> siteIds;
  private final Set<Long> vehicleIds;
  private final Set<Long> contractIds;
  private final Set<Long> userIds;

  private CollaborationAccessScope(
      boolean tenantWideAccess,
      Long currentUserId,
      Collection<Long> orgIds,
      Collection<Long> projectIds,
      Collection<Long> siteIds,
      Collection<Long> vehicleIds,
      Collection<Long> contractIds,
      Collection<Long> userIds) {
    this.tenantWideAccess = tenantWideAccess;
    this.currentUserId = currentUserId;
    this.orgIds = immutableCopy(orgIds);
    this.projectIds = immutableCopy(projectIds);
    this.siteIds = immutableCopy(siteIds);
    this.vehicleIds = immutableCopy(vehicleIds);
    this.contractIds = immutableCopy(contractIds);
    this.userIds = immutableCopy(userIds);
  }

  public static CollaborationAccessScope tenantWide(Long currentUserId) {
    return new CollaborationAccessScope(
        true, currentUserId, Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of());
  }

  public static CollaborationAccessScope scoped(
      Long currentUserId,
      Collection<Long> orgIds,
      Collection<Long> projectIds,
      Collection<Long> siteIds,
      Collection<Long> vehicleIds,
      Collection<Long> contractIds,
      Collection<Long> userIds) {
    return new CollaborationAccessScope(
        false, currentUserId, orgIds, projectIds, siteIds, vehicleIds, contractIds, userIds);
  }

  public static CollaborationAccessScope none(Long currentUserId) {
    return new CollaborationAccessScope(
        false, currentUserId, Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of());
  }

  public boolean isTenantWideAccess() {
    return tenantWideAccess;
  }

  public Long getCurrentUserId() {
    return currentUserId;
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

  public Set<Long> getVehicleIds() {
    return vehicleIds;
  }

  public Set<Long> getContractIds() {
    return contractIds;
  }

  public Set<Long> getUserIds() {
    return userIds;
  }

  public boolean hasAnyAccess() {
    return tenantWideAccess
        || currentUserId != null
        || !orgIds.isEmpty()
        || !projectIds.isEmpty()
        || !siteIds.isEmpty()
        || !vehicleIds.isEmpty()
        || !contractIds.isEmpty()
        || !userIds.isEmpty();
  }

  public boolean canAccessProject(Long projectId) {
    return tenantWideAccess || (projectId != null && projectIds.contains(projectId));
  }

  public boolean canAccessSite(Long siteId) {
    return tenantWideAccess || (siteId != null && siteIds.contains(siteId));
  }

  public boolean canAccessVehicle(Long vehicleId) {
    return tenantWideAccess || (vehicleId != null && vehicleIds.contains(vehicleId));
  }

  public boolean canAccessContract(Long contractId) {
    return tenantWideAccess || (contractId != null && contractIds.contains(contractId));
  }

  public boolean canAccessUser(Long userId) {
    return tenantWideAccess
        || (userId != null && (userIds.contains(userId) || userId.equals(currentUserId)));
  }

  public boolean matchesAlert(AlertEvent entity) {
    if (tenantWideAccess) {
      return true;
    }
    if (entity == null) {
      return false;
    }
    return canAccessProject(entity.getProjectId())
        || canAccessSite(entity.getSiteId())
        || canAccessVehicle(entity.getVehicleId())
        || canAccessContract(entity.getContractId())
        || canAccessUser(entity.getUserId())
        || matchesTypedReference(entity.getTargetType(), entity.getTargetId())
        || matchesTypedReference(entity.getRelatedType(), entity.getRelatedId());
  }

  public boolean matchesManualEvent(ManualEvent entity) {
    if (tenantWideAccess) {
      return true;
    }
    if (entity == null) {
      return false;
    }
    return canAccessProject(entity.getProjectId())
        || canAccessSite(entity.getSiteId())
        || canAccessVehicle(entity.getVehicleId())
        || canAccessUser(entity.getReporterId());
  }

  public boolean matchesSecurityInspection(SecurityInspection entity) {
    if (tenantWideAccess) {
      return true;
    }
    if (entity == null) {
      return false;
    }
    return canAccessProject(entity.getProjectId())
        || canAccessSite(entity.getSiteId())
        || canAccessVehicle(entity.getVehicleId())
        || canAccessUser(entity.getUserId())
        || canAccessUser(entity.getInspectorId())
        || matchesTypedReference(entity.getObjectType(), entity.getObjectId());
  }

  private boolean matchesTypedReference(String type, Long id) {
    if (id == null || type == null || type.isBlank()) {
      return false;
    }
    String normalized = type.trim().toUpperCase();
    return switch (normalized) {
      case "PROJECT" -> canAccessProject(id);
      case "SITE" -> canAccessSite(id);
      case "VEHICLE" -> canAccessVehicle(id);
      case "CONTRACT" -> canAccessContract(id);
      case "USER", "PERSON", "REPORTER", "INSPECTOR" -> canAccessUser(id);
      default -> false;
    };
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
