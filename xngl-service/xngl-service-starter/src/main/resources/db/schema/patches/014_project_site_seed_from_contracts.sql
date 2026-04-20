-- ============================================================
-- 014: 基于合同主数据回填项目/场地主档，保障项目/场地列表与报表有数据源
-- ============================================================

INSERT INTO biz_project (
  id,
  name,
  code,
  address,
  status,
  org_id,
  deleted
)
SELECT
  seeded.project_id,
  CONCAT('项目-', LPAD(seeded.project_id, 3, '0')),
  CONCAT('PRJ-', LPAD(seeded.project_id, 3, '0')),
  NULL,
  1,
  seeded.org_id,
  0
FROM (
  SELECT
    c.project_id,
    COALESCE(MAX(c.construction_org_id), MAX(c.transport_org_id), MAX(c.site_operator_org_id)) AS org_id
  FROM biz_contract c
  WHERE c.deleted = 0
    AND c.project_id IS NOT NULL
  GROUP BY c.project_id
) seeded
LEFT JOIN biz_project p ON p.id = seeded.project_id
WHERE p.id IS NULL;

INSERT INTO biz_site (
  id,
  name,
  code,
  address,
  project_id,
  status,
  org_id,
  deleted
)
SELECT
  seeded.site_id,
  CONCAT('场地-', LPAD(seeded.site_id, 3, '0')),
  CONCAT('SITE-', LPAD(seeded.site_id, 3, '0')),
  NULL,
  seeded.project_id,
  1,
  seeded.org_id,
  0
FROM (
  SELECT
    c.site_id,
    MAX(c.project_id) AS project_id,
    COALESCE(MAX(c.site_operator_org_id), MAX(c.construction_org_id), MAX(c.transport_org_id)) AS org_id
  FROM biz_contract c
  WHERE c.deleted = 0
    AND c.site_id IS NOT NULL
  GROUP BY c.site_id
) seeded
LEFT JOIN biz_site s ON s.id = seeded.site_id
WHERE s.id IS NULL;
