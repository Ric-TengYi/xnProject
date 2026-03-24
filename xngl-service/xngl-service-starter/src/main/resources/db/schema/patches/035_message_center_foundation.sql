CREATE TABLE IF NOT EXISTS biz_message_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  receiver_type VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT '接收对象类型',
  receiver_id BIGINT NULL COMMENT '接收对象ID',
  title VARCHAR(200) NOT NULL COMMENT '消息标题',
  content VARCHAR(1000) NULL COMMENT '消息内容',
  category VARCHAR(64) NULL COMMENT '消息分类',
  channel VARCHAR(32) NOT NULL DEFAULT 'SYSTEM' COMMENT '消息渠道',
  status VARCHAR(32) NOT NULL DEFAULT 'UNREAD' COMMENT '消息状态',
  priority VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '优先级',
  link_url VARCHAR(255) NULL COMMENT '跳转链接',
  biz_type VARCHAR(64) NULL COMMENT '业务类型',
  biz_id VARCHAR(64) NULL COMMENT '业务ID',
  sender_name VARCHAR(64) NULL COMMENT '发送人',
  send_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  read_time DATETIME NULL COMMENT '已读时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  KEY idx_message_tenant_receiver (tenant_id, receiver_type, receiver_id),
  KEY idx_message_tenant_status_time (tenant_id, status, send_time),
  KEY idx_message_tenant_category_time (tenant_id, category, send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息中心记录';

INSERT INTO biz_message_record (
  tenant_id, receiver_type, receiver_id, title, content, category, channel, status, priority, link_url, biz_type, biz_id, sender_name, send_time, read_time
)
SELECT 1, 'USER', 6, '合同审批已通过', '渣土消纳合同 HT-001 已完成审批，请及时查看后续流程。', '审批通知', 'SYSTEM', 'UNREAD', 'HIGH', '/contracts/39', 'CONTRACT', '39', '系统', DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL
FROM dual
WHERE NOT EXISTS (
  SELECT 1 FROM biz_message_record WHERE tenant_id = 1 AND receiver_id = 6 AND title = '合同审批已通过'
);

INSERT INTO biz_message_record (
  tenant_id, receiver_type, receiver_id, title, content, category, channel, status, priority, link_url, biz_type, biz_id, sender_name, send_time, read_time
)
SELECT 1, 'USER', 6, '项目预警提醒', '项目-001 剩余可用方量接近阈值，请关注调度安排。', '预警消息', 'SYSTEM', 'READ', 'NORMAL', '/alerts', 'PROJECT_ALERT', '1', '预警中心', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 20 HOUR)
FROM dual
WHERE NOT EXISTS (
  SELECT 1 FROM biz_message_record WHERE tenant_id = 1 AND receiver_id = 6 AND title = '项目预警提醒'
);

INSERT INTO biz_message_record (
  tenant_id, receiver_type, receiver_id, title, content, category, channel, status, priority, link_url, biz_type, biz_id, sender_name, send_time, read_time
)
SELECT 1, 'ALL', NULL, '平台维护通知', '今晚 23:00-23:30 进行平台维护，请提前保存业务操作。', '系统通知', 'SYSTEM', 'UNREAD', 'NORMAL', '/dashboard', 'SYSTEM_NOTICE', 'OPS-20260321', '平台运维', DATE_SUB(NOW(), INTERVAL 3 HOUR), NULL
FROM dual
WHERE NOT EXISTS (
  SELECT 1 FROM biz_message_record WHERE tenant_id = 1 AND title = '平台维护通知'
);
