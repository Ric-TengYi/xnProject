-- ============================================================
-- 033: 组织类型字典种子
-- ============================================================

INSERT INTO sys_data_dict
  (tenant_id, dict_type, dict_code, dict_label, dict_value, sort, status, remark, create_time, update_time)
SELECT 1, 'ORG_CATEGORY', 'LAW_ENFORCEMENT', '执法组织', 'LAW_ENFORCEMENT', 10, 'ENABLED', '默认执法组织类型', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM sys_data_dict WHERE tenant_id = 1 AND dict_type = 'ORG_CATEGORY' AND dict_code = 'LAW_ENFORCEMENT'
);

INSERT INTO sys_data_dict
  (tenant_id, dict_type, dict_code, dict_label, dict_value, sort, status, remark, create_time, update_time)
SELECT 1, 'ORG_CATEGORY', 'COMPANY_UNIT', '公司单位组织', 'COMPANY_UNIT', 20, 'ENABLED', '默认公司单位组织类型', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM sys_data_dict WHERE tenant_id = 1 AND dict_type = 'ORG_CATEGORY' AND dict_code = 'COMPANY_UNIT'
);
