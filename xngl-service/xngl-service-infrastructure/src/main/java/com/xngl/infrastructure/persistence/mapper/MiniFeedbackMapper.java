package com.xngl.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.infrastructure.persistence.entity.miniprogram.MiniFeedback;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MiniFeedbackMapper extends BaseMapper<MiniFeedback> {}
