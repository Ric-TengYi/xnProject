package com.xngl.manager.site;

import com.xngl.infrastructure.persistence.entity.site.Site;
import java.util.List;

/**
 * 消纳场地服务。提供场地列表与详情查询，供前端消纳清单页、场地下拉等使用。
 */
public interface SiteService {

  /**
   * 查询场地列表（不分页，用于下拉与看板）。
   */
  List<Site> list();

  /**
   * 按 ID 查询场地详情。
   */
  Site getById(Long id);
}
