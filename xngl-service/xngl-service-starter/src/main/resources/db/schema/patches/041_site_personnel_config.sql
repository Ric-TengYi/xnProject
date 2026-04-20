CREATE TABLE IF NOT EXISTS biz_site_personnel_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  site_id BIGINT NOT NULL COMMENT '场地ID',
  user_id BIGINT NOT NULL COMMENT '系统用户ID',
  org_id BIGINT NULL COMMENT '所属组织ID',
  role_type VARCHAR(64) NOT NULL DEFAULT 'SITE_MANAGER' COMMENT '场地岗位类型',
  duty_scope VARCHAR(255) NULL COMMENT '职责范围',
  shift_group VARCHAR(64) NULL COMMENT '班次分组',
  account_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用场地账号',
  remark VARCHAR(255) NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_site_personnel_user (tenant_id, site_id, user_id, deleted),
  KEY idx_site_personnel_site (tenant_id, site_id),
  KEY idx_site_personnel_org (tenant_id, org_id),
  KEY idx_site_personnel_role (tenant_id, role_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场地人员配置';

INSERT INTO biz_site_personnel_config (
  tenant_id, site_id, user_id, org_id, role_type, duty_scope, shift_group, account_enabled, remark
)
SELECT
  1,
  s.id,
  u.id,
  u.main_org_id,
  CASE
    WHEN s.id % 3 = 1 THEN 'SITE_MANAGER'
    WHEN s.id % 3 = 2 THEN 'SCALE_OPERATOR'
    ELSE 'SAFETY_OFFICER'
  END,
  CASE
    WHEN s.id % 3 = 1 THEN '场地值守、账号授权'
    WHEN s.id % 3 = 2 THEN '地磅操作、人工消纳复核'
    ELSE '现场安全巡检、设施检查'
  END,
  CASE
    WHEN s.id % 2 = 0 THEN '白班'
    ELSE '全天值守'
  END,
  1,
  '系统初始化场地人员配置'
FROM biz_site s
JOIN sys_user u
  ON u.tenant_id = 1
 AND u.deleted = 0
 AND u.status = 'ENABLED'
WHERE s.deleted = 0
  AND s.id <= 3
  AND u.username = 'admin'
  AND NOT EXISTS (
    SELECT 1
    FROM biz_site_personnel_config t
    WHERE t.tenant_id = 1
      AND t.site_id = s.id
      AND t.user_id = u.id
      AND t.deleted = 0
  );
