-- ============================================================
-- 051: 油电卡流水与消费确认
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_vehicle_card_transaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  card_id BIGINT NOT NULL,
  card_no VARCHAR(64) NOT NULL,
  card_type VARCHAR(32) NOT NULL COMMENT 'FUEL/ELECTRIC',
  txn_type VARCHAR(32) NOT NULL COMMENT 'RECHARGE/CONSUME',
  org_id BIGINT NULL,
  vehicle_id BIGINT NULL,
  amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  balance_before DECIMAL(18,2) NOT NULL DEFAULT 0,
  balance_after DECIMAL(18,2) NOT NULL DEFAULT 0,
  occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  operator_name VARCHAR(64) NULL,
  remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  KEY idx_vehicle_card_txn_card (tenant_id, card_id),
  KEY idx_vehicle_card_txn_type (tenant_id, txn_type),
  KEY idx_vehicle_card_txn_vehicle (vehicle_id),
  KEY idx_vehicle_card_txn_occurred (occurred_at)
) COMMENT='油电卡流水记录';

INSERT INTO biz_vehicle_card_transaction (
  tenant_id, card_id, card_no, card_type, txn_type, org_id, vehicle_id,
  amount, balance_before, balance_after, occurred_at, operator_name, remark
)
SELECT
  c.tenant_id,
  c.id,
  c.card_no,
  c.card_type,
  'RECHARGE',
  c.org_id,
  c.vehicle_id,
  3000.00,
  2200.50,
  5200.50,
  DATE_SUB(NOW(), INTERVAL 10 DAY),
  '系统初始化',
  '演示充值流水'
FROM biz_vehicle_card c
WHERE c.tenant_id = 1
  AND c.card_no = 'CARD-FUEL-0001'
  AND NOT EXISTS (
    SELECT 1
    FROM biz_vehicle_card_transaction t
    WHERE t.tenant_id = c.tenant_id
      AND t.card_id = c.id
      AND t.txn_type = 'RECHARGE'
      AND ABS(t.amount - 3000.00) < 0.001
  );

INSERT INTO biz_vehicle_card_transaction (
  tenant_id, card_id, card_no, card_type, txn_type, org_id, vehicle_id,
  amount, balance_before, balance_after, occurred_at, operator_name, remark
)
SELECT
  c.tenant_id,
  c.id,
  c.card_no,
  c.card_type,
  'CONSUME',
  c.org_id,
  c.vehicle_id,
  680.00,
  830.00,
  150.00,
  DATE_SUB(NOW(), INTERVAL 3 DAY),
  '调度中心',
  '演示用电确认'
FROM biz_vehicle_card c
WHERE c.tenant_id = 1
  AND c.card_no = 'CARD-ELEC-0002'
  AND NOT EXISTS (
    SELECT 1
    FROM biz_vehicle_card_transaction t
    WHERE t.tenant_id = c.tenant_id
      AND t.card_id = c.id
      AND t.txn_type = 'CONSUME'
      AND ABS(t.amount - 680.00) < 0.001
  );

INSERT INTO biz_vehicle_card_transaction (
  tenant_id, card_id, card_no, card_type, txn_type, org_id, vehicle_id,
  amount, balance_before, balance_after, occurred_at, operator_name, remark
)
SELECT
  c.tenant_id,
  c.id,
  c.card_no,
  c.card_type,
  'RECHARGE',
  c.org_id,
  c.vehicle_id,
  1200.00,
  2200.00,
  3400.00,
  DATE_SUB(NOW(), INTERVAL 5 DAY),
  '财务复核',
  '演示补能充值'
FROM biz_vehicle_card c
WHERE c.tenant_id = 1
  AND c.card_no = 'CARD-ELEC-0004'
  AND NOT EXISTS (
    SELECT 1
    FROM biz_vehicle_card_transaction t
    WHERE t.tenant_id = c.tenant_id
      AND t.card_id = c.id
      AND t.txn_type = 'RECHARGE'
      AND ABS(t.amount - 1200.00) < 0.001
  );
