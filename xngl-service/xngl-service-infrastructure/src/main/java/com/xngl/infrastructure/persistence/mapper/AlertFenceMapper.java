package com.xngl.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.infrastructure.persistence.entity.alert.AlertFence;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AlertFenceMapper extends BaseMapper<AlertFence> {}
