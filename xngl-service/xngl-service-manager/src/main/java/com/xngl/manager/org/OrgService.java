package com.xngl.manager.org;

import com.xngl.infrastructure.persistence.entity.organization.Org;

import java.util.List;

public interface OrgService {

  Org getById(Long id);

  List<Org> listTree(Long tenantId, String keyword, String status);

  long create(Org org);

  void update(Org org);

  void updateLeader(Long id, Long leaderUserId);

  void updateStatus(Long id, String status);

  void delete(Long id);
}
