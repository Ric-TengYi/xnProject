package com.xngl.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.infrastructure.persistence.entity.fleet.FleetDispatchOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FleetDispatchOrderMapper extends BaseMapper<FleetDispatchOrder> {}
