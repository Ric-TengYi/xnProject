package com.xngl.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.infrastructure.persistence.entity.vehicle.VehicleMaintenancePlan;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VehicleMaintenancePlanMapper extends BaseMapper<VehicleMaintenancePlan> {}
