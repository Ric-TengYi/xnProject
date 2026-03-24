package com.xngl.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.infrastructure.persistence.entity.system.PlatformIntegrationConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlatformIntegrationConfigMapper extends BaseMapper<PlatformIntegrationConfig> {}
