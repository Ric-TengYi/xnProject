package com.xngl.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.infrastructure.persistence.entity.site.Site;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SiteMapper extends BaseMapper<Site> {}
