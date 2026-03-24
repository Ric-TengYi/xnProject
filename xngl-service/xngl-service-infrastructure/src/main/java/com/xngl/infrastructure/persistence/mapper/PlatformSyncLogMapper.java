package com.xngl.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.infrastructure.persistence.entity.system.PlatformSyncLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlatformSyncLogMapper extends BaseMapper<PlatformSyncLog> {}
