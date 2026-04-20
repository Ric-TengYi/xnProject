package com.xngl.manager.approval;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.system.ApprovalActorRule;
import com.xngl.infrastructure.persistence.mapper.ApprovalActorRuleMapper;
import java.util.List;
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
      Long tenantId, String keyword, String processKey, String status, int pageNo, int pageSize) {
    return approvalActorRuleMapper.selectPage(
        new Page<>(pageNo, pageSize), buildQuery(tenantId, keyword, processKey, status));
  }

  @Override
  public List<ApprovalActorRule> list(Long tenantId, String keyword, String processKey, String status) {
    return approvalActorRuleMapper.selectList(buildQuery(tenantId, keyword, processKey, status));
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

  private LambdaQueryWrapper<ApprovalActorRule> buildQuery(
      Long tenantId, String keyword, String processKey, String status) {
    LambdaQueryWrapper<ApprovalActorRule> q = new LambdaQueryWrapper<>();
    if (tenantId != null) {
      q.eq(ApprovalActorRule::getTenantId, tenantId);
    }
    if (StringUtils.hasText(processKey) && !"all".equalsIgnoreCase(processKey)) {
      q.eq(ApprovalActorRule::getProcessKey, processKey.trim());
    }
    if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
      q.eq(ApprovalActorRule::getStatus, status.trim().toUpperCase());
    }
    if (StringUtils.hasText(keyword)) {
      String effectiveKeyword = keyword.trim();
      q.and(
          wrapper ->
              wrapper.like(ApprovalActorRule::getRuleName, effectiveKeyword)
                  .or()
                  .like(ApprovalActorRule::getRuleType, effectiveKeyword)
                  .or()
                  .like(ApprovalActorRule::getRuleExpression, effectiveKeyword)
                  .or()
                  .like(ApprovalActorRule::getProcessKey, effectiveKeyword));
    }
    q.orderByAsc(ApprovalActorRule::getPriority).orderByAsc(ApprovalActorRule::getId);
    return q;
  }
}
