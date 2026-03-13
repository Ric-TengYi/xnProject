package com.xngl.manager.org;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class OrgServiceImpl implements OrgService {

  private final OrgMapper orgMapper;

  public OrgServiceImpl(OrgMapper orgMapper) {
    this.orgMapper = orgMapper;
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
  public long create(Org org) {
    orgMapper.insert(org);
    return org.getId();
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
}
