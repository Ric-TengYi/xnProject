-- ============================================================
-- 020: 单位主数据增强与种子
-- ============================================================

ALTER TABLE sys_org ADD COLUMN contact_person VARCHAR(64) NULL COMMENT '联系人';
ALTER TABLE sys_org ADD COLUMN contact_phone VARCHAR(32) NULL COMMENT '联系电话';
ALTER TABLE sys_org ADD COLUMN address VARCHAR(255) NULL COMMENT '联系地址';
ALTER TABLE sys_org ADD COLUMN unified_social_code VARCHAR(64) NULL COMMENT '统一社会信用代码';
ALTER TABLE sys_org ADD COLUMN remark VARCHAR(500) NULL COMMENT '备注';

INSERT INTO sys_org (
  name,
  code,
  parent_id,
  sort_order,
  tenant_id,
  org_code,
  org_name,
  org_type,
  org_path,
  contact_person,
  contact_phone,
  address,
  unified_social_code,
  remark,
  status
)
SELECT
  '杭州市城建开发集团',
  'ORG-CONSTRUCT-HZCJ',
  1,
  11,
  1,
  'ORG-CONSTRUCT-HZCJ',
  '杭州市城建开发集团',
  'CONSTRUCTION_UNIT',
  '/1',
  '陈建华',
  '0571-86001111',
  '杭州市上城区市政路 88 号',
  '91330100HZCJ00001X',
  '建设单位演示数据',
  'ENABLED'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_org WHERE tenant_id = 1 AND org_code = 'ORG-CONSTRUCT-HZCJ'
);

INSERT INTO sys_org (
  name,
  code,
  parent_id,
  sort_order,
  tenant_id,
  org_code,
  org_name,
  org_type,
  org_path,
  contact_person,
  contact_phone,
  address,
  unified_social_code,
  remark,
  status
)
SELECT
  '浙江广源建设工程有限公司',
  'ORG-BUILDER-ZJGY',
  1,
  12,
  1,
  'ORG-BUILDER-ZJGY',
  '浙江广源建设工程有限公司',
  'BUILDER_UNIT',
  '/1',
  '王国强',
  '0571-86002222',
  '杭州市滨江区建设大道 66 号',
  '91330100ZJGY00002X',
  '施工单位演示数据',
  'ENABLED'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_org WHERE tenant_id = 1 AND org_code = 'ORG-BUILDER-ZJGY'
);

UPDATE sys_org
SET
  contact_person = COALESCE(contact_person, '张建国'),
  contact_phone = COALESCE(contact_phone, '13800138001'),
  address = COALESCE(address, '杭州市余杭区运输路 1 号'),
  unified_social_code = COALESCE(unified_social_code, CONCAT('91330100TRANS', LPAD(id, 4, '0'))),
  remark = COALESCE(remark, '运输单位演示数据')
WHERE tenant_id = 1
  AND org_type = 'TRANSPORT_COMPANY';

UPDATE biz_project p
JOIN sys_org o
  ON o.tenant_id = 1
 AND o.org_code = 'ORG-BUILDER-ZJGY'
SET p.org_id = o.id
WHERE p.id = 1
  AND (p.org_id IS NULL OR p.org_id = 1);

UPDATE biz_contract c
JOIN sys_org build_org
  ON build_org.tenant_id = c.tenant_id
 AND build_org.org_code = 'ORG-CONSTRUCT-HZCJ'
SET c.construction_org_id = build_org.id
WHERE c.tenant_id = 1
  AND (c.construction_org_id IS NULL OR c.construction_org_id = 1);

UPDATE biz_contract c
JOIN sys_org transport_org
  ON transport_org.tenant_id = c.tenant_id
 AND transport_org.org_code =
    CASE MOD(c.id, 4)
      WHEN 0 THEN 'ORG-TRANS-HJ'
      WHEN 1 THEN 'ORG-TRANS-SD'
      WHEN 2 THEN 'ORG-TRANS-JA'
      ELSE 'ORG-TRANS-XS'
    END
SET c.transport_org_id = transport_org.id
WHERE c.tenant_id = 1
  AND (c.transport_org_id IS NULL OR c.transport_org_id = 1);
