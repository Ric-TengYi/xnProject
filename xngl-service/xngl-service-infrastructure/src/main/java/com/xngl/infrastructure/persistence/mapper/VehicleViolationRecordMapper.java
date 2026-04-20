package com.xngl.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.infrastructure.persistence.entity.vehicle.VehicleViolationRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VehicleViolationRecordMapper extends BaseMapper<VehicleViolationRecord> {}
