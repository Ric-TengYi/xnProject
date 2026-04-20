-- ============================================================
-- 016: 回填演示用合同领票数据，供项目/场地报表与结算联调使用
-- ============================================================

INSERT INTO biz_contract_ticket (
  tenant_id,
  contract_id,
  ticket_no,
  ticket_type,
  ticket_date,
  amount,
  volume,
  status,
  remark,
  creator_id
)
SELECT
  c.tenant_id,
  c.id,
  CONCAT('TK-', c.id, '-20260305'),
  'DISPOSAL',
  DATE('2026-03-05'),
  ROUND(COALESCE(c.unit_price, c.contract_amount / NULLIF(c.agreed_volume, 0), 18.00) * 1200, 2),
  1200.00,
  'NORMAL',
  '自动回填的演示领票数据',
  COALESCE(c.applicant_id, 1)
FROM biz_contract c
WHERE c.deleted = 0
  AND c.id = (SELECT MIN(id) FROM biz_contract WHERE deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM biz_contract_ticket WHERE deleted = 0);

INSERT INTO biz_contract_ticket (
  tenant_id,
  contract_id,
  ticket_no,
  ticket_type,
  ticket_date,
  amount,
  volume,
  status,
  remark,
  creator_id
)
SELECT
  c.tenant_id,
  c.id,
  CONCAT('TK-', c.id, '-20260318'),
  'DISPOSAL',
  DATE('2026-03-18'),
  ROUND(COALESCE(c.unit_price, c.contract_amount / NULLIF(c.agreed_volume, 0), 18.00) * 1800, 2),
  1800.00,
  'NORMAL',
  '自动回填的演示领票数据',
  COALESCE(c.applicant_id, 1)
FROM biz_contract c
WHERE c.deleted = 0
  AND c.id = (
    SELECT id
    FROM (
      SELECT id FROM biz_contract WHERE deleted = 0 ORDER BY id LIMIT 1 OFFSET 1
    ) t
  )
  AND NOT EXISTS (SELECT 1 FROM biz_contract_ticket WHERE deleted = 0);

INSERT INTO biz_contract_ticket (
  tenant_id,
  contract_id,
  ticket_no,
  ticket_type,
  ticket_date,
  amount,
  volume,
  status,
  remark,
  creator_id
)
SELECT
  c.tenant_id,
  c.id,
  CONCAT('TK-', c.id, '-20260412'),
  'DISPOSAL',
  DATE('2026-04-12'),
  ROUND(COALESCE(c.unit_price, c.contract_amount / NULLIF(c.agreed_volume, 0), 18.00) * 2600, 2),
  2600.00,
  'NORMAL',
  '自动回填的演示领票数据',
  COALESCE(c.applicant_id, 1)
FROM biz_contract c
WHERE c.deleted = 0
  AND c.id = (SELECT MIN(id) FROM biz_contract WHERE deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM biz_contract_ticket WHERE deleted = 0);
