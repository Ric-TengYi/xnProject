-- 车辆 GPS 相关字段：大表，仅校对时提示，不自动执行 ALTER
-- 若需添加请在生产低峰期手动执行
-- 以下 ALTER 会被识别为大表补充，仅在校对时提示缺失，不执行

ALTER TABLE biz_vehicle ADD COLUMN lng DECIMAL(10,7) DEFAULT NULL COMMENT '经度';
ALTER TABLE biz_vehicle ADD COLUMN lat DECIMAL(10,7) DEFAULT NULL COMMENT '纬度';
ALTER TABLE biz_vehicle ADD COLUMN gps_time DATETIME DEFAULT NULL COMMENT '定位时间';
