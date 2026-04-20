package com.xngl.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.infrastructure.persistence.entity.site.SiteDevice;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SiteDeviceMapper extends BaseMapper<SiteDevice> {}
