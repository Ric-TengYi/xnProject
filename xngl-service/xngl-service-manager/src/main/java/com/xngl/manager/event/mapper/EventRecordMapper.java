package com.xngl.manager.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.manager.event.entity.EventRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EventRecordMapper extends BaseMapper<EventRecord> {
}