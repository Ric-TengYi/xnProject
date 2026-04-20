-- ============================================================
-- 015: 场地主数据增强与分类结算规则
-- ============================================================

ALTER TABLE biz_site ADD COLUMN site_type VARCHAR(32) NULL COMMENT '场地类型: STATE_OWNED/COLLECTIVE/ENGINEERING/SHORT_BARGE';
ALTER TABLE biz_site ADD COLUMN capacity DECIMAL(18,2) NULL DEFAULT 0 COMMENT '场地容量';
ALTER TABLE biz_site ADD COLUMN settlement_mode VARCHAR(32) NULL COMMENT '结算模式: MONTHLY_APPLY/RATIO_SERVICE_FEE/UNIT_PRICE/SERVICE_FEE';
ALTER TABLE biz_site ADD COLUMN disposal_unit_price DECIMAL(18,2) NULL DEFAULT 0 COMMENT '消纳费单价';
ALTER TABLE biz_site ADD COLUMN disposal_fee_rate DECIMAL(8,4) NULL DEFAULT 0 COMMENT '消纳费比例';
ALTER TABLE biz_site ADD COLUMN service_fee_unit_price DECIMAL(18,2) NULL DEFAULT 0 COMMENT '平台服务费单价';

CREATE INDEX IF NOT EXISTS idx_site_type_status ON biz_site(site_type, status);

UPDATE biz_site
SET
  site_type =
      CASE
        WHEN site_type IS NOT NULL AND site_type <> '' THEN site_type
        WHEN code LIKE 'GY%' OR MOD(id, 4) = 1 THEN 'STATE_OWNED'
        WHEN code LIKE 'JT%' OR MOD(id, 4) = 2 THEN 'COLLECTIVE'
        WHEN code LIKE 'GC%' OR MOD(id, 4) = 3 THEN 'ENGINEERING'
        ELSE 'SHORT_BARGE'
      END,
  capacity =
      CASE
        WHEN capacity IS NOT NULL AND capacity > 0 THEN capacity
        ELSE (((MOD(id, 7) + 3) * 100000))
      END,
  settlement_mode =
      CASE
        WHEN settlement_mode IS NOT NULL AND settlement_mode <> '' THEN settlement_mode
        WHEN code LIKE 'GY%' OR MOD(id, 4) = 1 THEN 'MONTHLY_APPLY'
        WHEN code LIKE 'JT%' OR MOD(id, 4) = 2 THEN 'RATIO_SERVICE_FEE'
        WHEN code LIKE 'GC%' OR MOD(id, 4) = 3 THEN 'UNIT_PRICE'
        ELSE 'SERVICE_FEE'
      END,
  disposal_unit_price =
      CASE
        WHEN disposal_unit_price IS NOT NULL AND disposal_unit_price > 0 THEN disposal_unit_price
        WHEN code LIKE 'GY%' OR MOD(id, 4) = 1 THEN 18.00
        WHEN code LIKE 'JT%' OR MOD(id, 4) = 2 THEN 16.00
        WHEN code LIKE 'GC%' OR MOD(id, 4) = 3 THEN 12.00
        ELSE 0.00
      END,
  disposal_fee_rate =
      CASE
        WHEN disposal_fee_rate IS NOT NULL AND disposal_fee_rate > 0 THEN disposal_fee_rate
        WHEN code LIKE 'JT%' OR MOD(id, 4) = 2 THEN 0.7000
        ELSE 0.0000
      END,
  service_fee_unit_price =
      CASE
        WHEN service_fee_unit_price IS NOT NULL AND service_fee_unit_price > 0 THEN service_fee_unit_price
        WHEN code LIKE 'JT%' OR MOD(id, 4) = 2 THEN 2.00
        WHEN code LIKE 'GC%' OR MOD(id, 4) = 3 THEN 0.00
        WHEN code LIKE 'GY%' OR MOD(id, 4) = 1 THEN 0.00
        ELSE 3.00
      END;
