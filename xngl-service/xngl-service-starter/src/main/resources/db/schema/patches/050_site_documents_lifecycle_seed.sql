INSERT INTO biz_site_document (
  tenant_id, site_id, stage_code, approval_type, document_type, file_name, file_url,
  file_size, mime_type, format_requirement, uploader_id, uploader_name, remark
)
SELECT 1, 1, 'CONSTRUCTION', 'CONSTRUCTION', 'CONSTRUCTION_PLAN',
  '场地建设方案.pdf', 'https://files.local/site-1/construction-plan.pdf',
  182400, 'application/pdf', 'PDF', 6, 'Local Admin', '建设阶段演示资料'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_site_document
  WHERE tenant_id = 1 AND site_id = 1 AND document_type = 'CONSTRUCTION_PLAN' AND deleted = 0
);

INSERT INTO biz_site_document (
  tenant_id, site_id, stage_code, approval_type, document_type, file_name, file_url,
  file_size, mime_type, format_requirement, uploader_id, uploader_name, remark
)
SELECT 1, 1, 'CONSTRUCTION', 'CONSTRUCTION', 'BOUNDARY_SURVEY',
  '场地红线测绘图.png', 'https://files.local/site-1/boundary-survey.png',
  94320, 'image/png', 'PDF,JPG,PNG', 6, 'Local Admin', '建设阶段演示资料'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_site_document
  WHERE tenant_id = 1 AND site_id = 1 AND document_type = 'BOUNDARY_SURVEY' AND deleted = 0
);

INSERT INTO biz_site_document (
  tenant_id, site_id, stage_code, approval_type, document_type, file_name, file_url,
  file_size, mime_type, format_requirement, uploader_id, uploader_name, remark
)
SELECT 1, 1, 'TRANSFER', 'TRANSFER', 'TRANSFER_ACCEPTANCE',
  '移交验收资料.pdf', 'https://files.local/site-1/transfer-acceptance.pdf',
  118600, 'application/pdf', 'PDF', 6, 'Local Admin', '移交阶段演示资料'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_site_document
  WHERE tenant_id = 1 AND site_id = 1 AND document_type = 'TRANSFER_ACCEPTANCE' AND deleted = 0
);
