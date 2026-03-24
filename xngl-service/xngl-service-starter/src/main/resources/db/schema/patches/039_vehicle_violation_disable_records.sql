CREATE TABLE IF NOT EXISTS biz_vehicle_violation_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  vehicle_id BIGINT NOT NULL COMMENT '车辆ID',
  plate_no VARCHAR(20) NOT NULL COMMENT '车牌号',
  org_id BIGINT NULL COMMENT '所属单位ID',
  violation_type VARCHAR(64) NOT NULL COMMENT '违规类型',
  trigger_time DATETIME NOT NULL COMMENT '触发时间',
  trigger_location VARCHAR(255) NULL COMMENT '触发地点',
  action_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态',
  penalty_result VARCHAR(255) NULL COMMENT '处罚结果',
  ban_start_time DATETIME NULL COMMENT '禁用开始时间',
  ban_end_time DATETIME NULL COMMENT '禁用结束时间',
  release_time DATETIME NULL COMMENT '解禁时间',
  release_reason VARCHAR(255) NULL COMMENT '解禁原因',
  operator_name VARCHAR(64) NULL COMMENT '操作人',
  remark VARCHAR(255) NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  KEY idx_vehicle_violation_tenant_status_time (tenant_id, action_status, trigger_time),
  KEY idx_vehicle_violation_vehicle (tenant_id, vehicle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆违规与禁用记录';

INSERT INTO biz_vehicle_violation_record (
  tenant_id, vehicle_id, plate_no, org_id, violation_type, trigger_time, trigger_location, action_status,
  penalty_result, operator_name, remark
)
SELECT 1, 1, '浙A12345', 6, '偏航预警', DATE_SUB(NOW(), INTERVAL 2 DAY), '科技路与创新大道交汇处', 'PENDING',
  '待人工确认处理', '系统', '待处理中违规记录'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_vehicle_violation_record WHERE tenant_id = 1 AND vehicle_id = 1 AND violation_type = '偏航预警'
);

INSERT INTO biz_vehicle_violation_record (
  tenant_id, vehicle_id, plate_no, org_id, violation_type, trigger_time, trigger_location, action_status,
  penalty_result, operator_name, remark
)
SELECT 1, 4, '浙A22345', 7, '未打卡入场', DATE_SUB(NOW(), INTERVAL 4 DAY), '场地-001', 'PROCESSED',
  '警告教育', '调度员', '已处理示例'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_vehicle_violation_record WHERE tenant_id = 1 AND vehicle_id = 4 AND violation_type = '未打卡入场'
);

INSERT INTO biz_vehicle_violation_record (
  tenant_id, vehicle_id, plate_no, org_id, violation_type, trigger_time, trigger_location, action_status,
  penalty_result, ban_start_time, ban_end_time, operator_name, remark
)
SELECT 1, 3, '浙A12347', 6, '证件过期', DATE_SUB(NOW(), INTERVAL 3 DAY), '城东一号消纳场', 'DISABLED',
  '停运3天', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), '安全管理员', '当前禁用中'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_vehicle_violation_record WHERE tenant_id = 1 AND vehicle_id = 3 AND action_status = 'DISABLED'
);

INSERT INTO biz_vehicle_violation_record (
  tenant_id, vehicle_id, plate_no, org_id, violation_type, trigger_time, trigger_location, action_status,
  penalty_result, ban_start_time, ban_end_time, release_time, release_reason, operator_name, remark
)
SELECT 1, 5, '浙A22346', 7, '超速行驶', DATE_SUB(NOW(), INTERVAL 6 DAY), '绕城高速东段', 'RELEASED',
  '补训后恢复运营', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY),
  '已完成整改，提前解禁', '安全管理员', '已解禁示例'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_vehicle_violation_record WHERE tenant_id = 1 AND vehicle_id = 5 AND action_status = 'RELEASED'
);
