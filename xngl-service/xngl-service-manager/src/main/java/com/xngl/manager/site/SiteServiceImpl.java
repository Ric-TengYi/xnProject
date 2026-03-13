package com.xngl.manager.site;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SiteServiceImpl implements SiteService {

  private final SiteMapper siteMapper;

  public SiteServiceImpl(SiteMapper siteMapper) {
    this.siteMapper = siteMapper;
  }

  @Override
  public List<Site> list() {
    return siteMapper.selectList(
        new LambdaQueryWrapper<Site>().orderByAsc(Site::getId));
  }

  @Override
  public Site getById(Long id) {
    return siteMapper.selectById(id);
  }
}
