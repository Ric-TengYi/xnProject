-- ============================================================
-- 018: 处置证表与演示数据
-- ============================================================

CREATE TABLE IF NOT EXISTS disposal_permit (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  permit_no VARCHAR(64) NOT NULL,
  permit_type VARCHAR(32) NULL COMMENT '证件类型: DISPOSAL/TRANSPORT',
  project_id BIGINT NULL,
  site_id BIGINT NULL,
  vehicle_no VARCHAR(32) NULL,
  issue_date DATE NULL,
  expire_date DATE NULL,
  approved_volume DECIMAL(18,2) NOT NULL DEFAULT 0,
  used_volume DECIMAL(18,2) NOT NULL DEFAULT 0,
  status VARCHAR(32) NULL,
  bind_status VARCHAR(32) NULL COMMENT '绑定状态: BOUND/UNBOUND',
  remark VARCHAR(500) NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_disposal_permit_no (permit_no),
  KEY idx_disposal_permit_project (project_id),
  KEY idx_disposal_permit_site (site_id),
  KEY idx_disposal_permit_status (status, expire_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处置证';

INSERT INTO disposal_permit (
  permit_no,
  permit_type,
  project_id,
  site_id,
  vehicle_no,
  issue_date,
  expire_date,
  approved_volume,
  used_volume,
  status,
  bind_status,
  remark
)
SELECT
  'PZ-2026-001',
  'DISPOSAL',
  c.project_id,
  c.site_id,
  '浙A12345',
  DATE('2026-03-01'),
  DATE('2026-12-31'),
  8000.00,
  1200.00,
  'ACTIVE',
  'BOUND',
  '系统回填的处置证演示数据'
FROM biz_contract c
WHERE c.deleted = 0
  AND c.id = (SELECT MIN(id) FROM biz_contract WHERE deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM disposal_permit WHERE permit_no = 'PZ-2026-001');

INSERT INTO disposal_permit (
  permit_no,
  permit_type,
  project_id,
  site_id,
  vehicle_no,
  issue_date,
  expire_date,
  approved_volume,
  used_volume,
  status,
  bind_status,
  remark
)
SELECT
  'PZ-2026-002',
  'TRANSPORT',
  c.project_id,
  c.site_id,
  NULL,
  DATE('2026-04-01'),
  DATE('2026-06-30'),
  5000.00,
  2600.00,
  'EXPIRING',
  'UNBOUND',
  '系统回填的准运证演示数据'
FROM biz_contract c
WHERE c.deleted = 0
  AND c.id = (SELECT MIN(id) FROM biz_contract WHERE deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM disposal_permit WHERE permit_no = 'PZ-2026-002');
