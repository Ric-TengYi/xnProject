CREATE TABLE IF NOT EXISTS mini_manual_disposal_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  site_id BIGINT NOT NULL COMMENT '场地ID',
  contract_id BIGINT NOT NULL COMMENT '合同ID',
  project_id BIGINT NULL COMMENT '项目ID',
  vehicle_id BIGINT NULL COMMENT '车辆ID',
  user_id BIGINT NOT NULL COMMENT '上报人ID',
  reporter_name VARCHAR(64) NOT NULL COMMENT '上报人名称',
  plate_no VARCHAR(32) NULL COMMENT '车牌号',
  disposal_time DATETIME NOT NULL COMMENT '消纳时间',
  volume DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '消纳方量',
  amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '消纳金额',
  weight_tons DECIMAL(12,2) NULL COMMENT '重量吨位',
  photo_urls VARCHAR(2000) NULL COMMENT '照片地址',
  remark VARCHAR(500) NULL COMMENT '备注',
  status VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED' COMMENT '状态',
  ticket_id BIGINT NULL COMMENT '关联合同消纳记录ID',
  source_channel VARCHAR(32) NOT NULL DEFAULT 'MINI' COMMENT '来源渠道',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  KEY idx_mini_manual_disposal_site (tenant_id, site_id, disposal_time),
  KEY idx_mini_manual_disposal_contract (tenant_id, contract_id, disposal_time),
  KEY idx_mini_manual_disposal_user (tenant_id, user_id, disposal_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序手动消纳记录';

CREATE TABLE IF NOT EXISTS mini_vehicle_inspection (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  vehicle_id BIGINT NOT NULL COMMENT '车辆ID',
  org_id BIGINT NULL COMMENT '车辆所属单位ID',
  user_id BIGINT NOT NULL COMMENT '检查人ID',
  inspector_name VARCHAR(64) NOT NULL COMMENT '检查人',
  plate_no VARCHAR(32) NOT NULL COMMENT '车牌号',
  dispatch_no VARCHAR(64) NULL COMMENT '调度单号',
  inspection_time DATETIME NOT NULL COMMENT '检查时间',
  vehicle_photo_urls VARCHAR(2000) NULL COMMENT '车辆状况照片',
  certificate_photo_urls VARCHAR(2000) NULL COMMENT '证件照片',
  issue_summary VARCHAR(500) NULL COMMENT '问题说明',
  conclusion VARCHAR(32) NOT NULL DEFAULT 'PASS' COMMENT '检查结论',
  status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  KEY idx_mini_vehicle_inspection_vehicle (tenant_id, vehicle_id, inspection_time),
  KEY idx_mini_vehicle_inspection_user (tenant_id, user_id, inspection_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序车辆检查记录';

CREATE TABLE IF NOT EXISTS mini_safety_course (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  course_code VARCHAR(64) NOT NULL COMMENT '课程编码',
  title VARCHAR(128) NOT NULL COMMENT '课程标题',
  course_type VARCHAR(32) NOT NULL DEFAULT 'VIDEO' COMMENT '课程类型',
  cover_url VARCHAR(500) NULL COMMENT '封面地址',
  file_url VARCHAR(500) NOT NULL COMMENT '课件地址',
  duration_minutes INT NOT NULL DEFAULT 10 COMMENT '建议学习时长',
  random_check_minutes INT NOT NULL DEFAULT 5 COMMENT '随机校验分钟数',
  face_check_required TINYINT NOT NULL DEFAULT 1 COMMENT '是否需要人脸校验',
  description VARCHAR(500) NULL COMMENT '课程说明',
  status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_mini_safety_course_code (tenant_id, course_code),
  KEY idx_mini_safety_course_status (tenant_id, status, course_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序安全教育课程';

CREATE TABLE IF NOT EXISTS mini_safety_learning_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  course_id BIGINT NOT NULL COMMENT '课程ID',
  user_id BIGINT NOT NULL COMMENT '学习人ID',
  learner_name VARCHAR(64) NOT NULL COMMENT '学习人',
  status VARCHAR(32) NOT NULL DEFAULT 'LEARNING' COMMENT '学习状态',
  studied_minutes INT NOT NULL DEFAULT 0 COMMENT '已学习分钟',
  progress_percent INT NOT NULL DEFAULT 0 COMMENT '进度百分比',
  face_check_count INT NOT NULL DEFAULT 0 COMMENT '人脸校验次数',
  last_face_check_time DATETIME NULL COMMENT '最近一次人脸校验时间',
  next_face_check_time DATETIME NULL COMMENT '下一次人脸校验时间',
  start_time DATETIME NOT NULL COMMENT '开始时间',
  complete_time DATETIME NULL COMMENT '完成时间',
  last_study_time DATETIME NULL COMMENT '最近学习时间',
  remark VARCHAR(500) NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  KEY idx_mini_safety_learning_user (tenant_id, user_id, status),
  KEY idx_mini_safety_learning_course (tenant_id, course_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序安全教育学习记录';

INSERT INTO mini_safety_course (
  tenant_id, course_code, title, course_type, cover_url, file_url,
  duration_minutes, random_check_minutes, face_check_required, description, status
)
SELECT
  1, 'SAFE-VIDEO-001', '渣土运输驾驶员班前安全教育', 'VIDEO',
  'https://files.local/safety/video-cover-001.jpg',
  'https://files.local/safety/safe-video-001.mp4',
  15, 5, 1, '覆盖班前检查、行车规范、进出场注意事项', 'PUBLISHED'
FROM dual
WHERE NOT EXISTS (
  SELECT 1 FROM mini_safety_course WHERE tenant_id = 1 AND course_code = 'SAFE-VIDEO-001'
);

INSERT INTO mini_safety_course (
  tenant_id, course_code, title, course_type, cover_url, file_url,
  duration_minutes, random_check_minutes, face_check_required, description, status
)
SELECT
  1, 'SAFE-DOC-001', '消纳场消防与应急设施巡查要点', 'DOCUMENT',
  'https://files.local/safety/doc-cover-001.jpg',
  'https://files.local/safety/safe-doc-001.pdf',
  10, 4, 1, '覆盖场地消防设施、应急物资与巡查要求', 'PUBLISHED'
FROM dual
WHERE NOT EXISTS (
  SELECT 1 FROM mini_safety_course WHERE tenant_id = 1 AND course_code = 'SAFE-DOC-001'
);
