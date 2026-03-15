package com.xngl.manager.safety.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.manager.safety.entity.SafetyViolation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SafetyViolationMapper extends BaseMapper<SafetyViolation> {
}