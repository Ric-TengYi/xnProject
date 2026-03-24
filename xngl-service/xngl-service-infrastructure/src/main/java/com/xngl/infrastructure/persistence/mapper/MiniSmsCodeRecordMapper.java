package com.xngl.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.infrastructure.persistence.entity.miniprogram.MiniSmsCodeRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MiniSmsCodeRecordMapper extends BaseMapper<MiniSmsCodeRecord> {}
