-- ============================================================
-- 017: 补充 4-6 月演示领票数据，支撑分类结算与项目结算联调
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
  CONCAT('TK-', c.id, '-20260412'),
  'DISPOSAL',
  DATE('2026-04-12'),
  ROUND(COALESCE(c.unit_price, c.contract_amount / NULLIF(c.agreed_volume, 0), 18.00) * 2600, 2),
  2600.00,
  'NORMAL',
  '自动回填的 4 月演示领票数据',
  COALESCE(c.applicant_id, 1)
FROM biz_contract c
WHERE c.deleted = 0
  AND c.id = (SELECT MIN(id) FROM biz_contract WHERE deleted = 0)
  AND NOT EXISTS (
    SELECT 1 FROM biz_contract_ticket t WHERE t.ticket_no = CONCAT('TK-', c.id, '-20260412')
  );

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
  CONCAT('TK-', c.id, '-20260608'),
  'DISPOSAL',
  DATE('2026-06-08'),
  ROUND(COALESCE(c.unit_price, c.contract_amount / NULLIF(c.agreed_volume, 0), 18.00) * 1500, 2),
  1500.00,
  'NORMAL',
  '自动回填的 6 月演示领票数据',
  COALESCE(c.applicant_id, 1)
FROM biz_contract c
WHERE c.deleted = 0
  AND c.id = (SELECT MIN(id) FROM biz_contract WHERE deleted = 0)
  AND NOT EXISTS (
    SELECT 1 FROM biz_contract_ticket t WHERE t.ticket_no = CONCAT('TK-', c.id, '-20260608')
  );
