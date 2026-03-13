package com.xngl.manager.approval;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.system.ApprovalActorRule;
import com.xngl.infrastructure.persistence.mapper.ApprovalActorRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ApprovalActorRuleServiceImpl implements ApprovalActorRuleService {

  private final ApprovalActorRuleMapper approvalActorRuleMapper;

  public ApprovalActorRuleServiceImpl(ApprovalActorRuleMapper approvalActorRuleMapper) {
    this.approvalActorRuleMapper = approvalActorRuleMapper;
  }

  @Override
  public ApprovalActorRule getById(Long id) {
    return approvalActorRuleMapper.selectById(id);
  }

  @Override
  public IPage<ApprovalActorRule> page(
      Long tenantId, String processKey, String status, int pageNo, int pageSize) {
    LambdaQueryWrapper<ApprovalActorRule> q = new LambdaQueryWrapper<>();
    if (tenantId != null) q.eq(ApprovalActorRule::getTenantId, tenantId);
    if (StringUtils.hasText(processKey)) q.eq(ApprovalActorRule::getProcessKey, processKey);
    if (StringUtils.hasText(status)) q.eq(ApprovalActorRule::getStatus, status);
    q.orderByAsc(ApprovalActorRule::getPriority);
    return approvalActorRuleMapper.selectPage(new Page<>(pageNo, pageSize), q);
  }

  @Override
  public long create(ApprovalActorRule rule) {
    approvalActorRuleMapper.insert(rule);
    return rule.getId();
  }

  @Override
  public void update(ApprovalActorRule rule) {
    approvalActorRuleMapper.updateById(rule);
  }

  @Override
  public void updateStatus(Long id, String status) {
    ApprovalActorRule r = new ApprovalActorRule();
    r.setId(id);
    r.setStatus(status);
    approvalActorRuleMapper.updateById(r);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void delete(Long id) {
    approvalActorRuleMapper.deleteById(id);
  }
}
