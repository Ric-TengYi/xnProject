package com.xngl.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.infrastructure.persistence.entity.project.ProjectConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectConfigMapper extends BaseMapper<ProjectConfig> {}
