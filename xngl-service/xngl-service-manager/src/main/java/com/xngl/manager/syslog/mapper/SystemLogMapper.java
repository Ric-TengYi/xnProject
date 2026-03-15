package com.xngl.manager.syslog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.manager.syslog.entity.SystemLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemLogMapper extends BaseMapper<SystemLog> {
}