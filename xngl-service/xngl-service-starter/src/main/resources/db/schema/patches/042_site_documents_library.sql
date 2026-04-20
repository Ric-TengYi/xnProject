CREATE TABLE IF NOT EXISTS biz_site_document (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  site_id BIGINT NOT NULL COMMENT '场地ID',
  stage_code VARCHAR(32) NOT NULL COMMENT '阶段编码: APPROVAL/OPERATION/TRANSFER',
  approval_type VARCHAR(64) NULL COMMENT '审批类型',
  document_type VARCHAR(64) NOT NULL COMMENT '资料类型编码',
  file_name VARCHAR(200) NOT NULL COMMENT '文件名',
  file_url VARCHAR(500) NOT NULL COMMENT '文件地址',
  file_size BIGINT NULL COMMENT '文件大小',
  mime_type VARCHAR(100) NULL COMMENT '文件类型',
  format_requirement VARCHAR(64) NULL COMMENT '格式要求',
  uploader_id BIGINT NULL COMMENT '上传人ID',
  uploader_name VARCHAR(64) NULL COMMENT '上传人名称',
  remark VARCHAR(255) NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  KEY idx_site_document_site (tenant_id, site_id, stage_code, create_time),
  KEY idx_site_document_type (tenant_id, document_type, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场地资料台账';

INSERT INTO biz_site_document (
  tenant_id, site_id, stage_code, approval_type, document_type, file_name, file_url,
  file_size, mime_type, format_requirement, uploader_id, uploader_name, remark
)
SELECT 1, 1, 'APPROVAL', 'EIA', 'EIA_APPROVAL', '环评批复报告.pdf', 'https://files.local/site-1/eia-approval.pdf',
  204800, 'application/pdf', 'PDF', 6, 'Local Admin', '默认演示资料'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_site_document
  WHERE tenant_id = 1 AND site_id = 1 AND document_type = 'EIA_APPROVAL' AND deleted = 0
);

INSERT INTO biz_site_document (
  tenant_id, site_id, stage_code, approval_type, document_type, file_name, file_url,
  file_size, mime_type, format_requirement, uploader_id, uploader_name, remark
)
SELECT 1, 1, 'APPROVAL', 'PROJECT', 'PROJECT_APPROVAL', '立项批复文件.pdf', 'https://files.local/site-1/project-approval.pdf',
  156000, 'application/pdf', 'PDF', 6, 'Local Admin', '默认演示资料'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_site_document
  WHERE tenant_id = 1 AND site_id = 1 AND document_type = 'PROJECT_APPROVAL' AND deleted = 0
);

INSERT INTO biz_site_document (
  tenant_id, site_id, stage_code, approval_type, document_type, file_name, file_url,
  file_size, mime_type, format_requirement, uploader_id, uploader_name, remark
)
SELECT 1, 1, 'OPERATION', 'SAFETY', 'SAFETY_INSPECTION', '2026年3月安全检查记录.docx', 'https://files.local/site-1/safety-inspection-202603.docx',
  88600, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'DOCX,PDF', 6, 'Local Admin', '默认演示资料'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_site_document
  WHERE tenant_id = 1 AND site_id = 1 AND document_type = 'SAFETY_INSPECTION' AND deleted = 0
);
