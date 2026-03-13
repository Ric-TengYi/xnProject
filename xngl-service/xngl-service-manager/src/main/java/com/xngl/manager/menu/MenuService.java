package com.xngl.manager.menu;

import com.xngl.infrastructure.persistence.entity.system.Menu;

import java.util.List;

public interface MenuService {

  Menu getById(Long id);

  List<Menu> listTree(Long tenantId, String keyword, String status);

  List<Menu> listByIds(List<Long> ids);

  long create(Menu menu);

  void update(Menu menu);

  void delete(Long id);
}
