package com.xngl.manager.menu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.system.Menu;
import com.xngl.infrastructure.persistence.mapper.MenuMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class MenuServiceImpl implements MenuService {

  private final MenuMapper menuMapper;

  public MenuServiceImpl(MenuMapper menuMapper) {
    this.menuMapper = menuMapper;
  }

  @Override
  public Menu getById(Long id) {
    return menuMapper.selectById(id);
  }

  @Override
  public List<Menu> listTree(Long tenantId, String keyword, String status) {
    if (tenantId == null) return List.of();
    LambdaQueryWrapper<Menu> q = new LambdaQueryWrapper<>();
    q.eq(Menu::getTenantId, tenantId);
    if (StringUtils.hasText(keyword)) {
      q.and(
          w ->
              w.like(Menu::getMenuCode, keyword)
                  .or()
                  .like(Menu::getMenuName, keyword));
    }
    if (StringUtils.hasText(status)) q.eq(Menu::getStatus, status);
    q.orderByAsc(Menu::getSortOrder);
    return menuMapper.selectList(q);
  }

  @Override
  public List<Menu> listByIds(List<Long> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return List.of();
    }
    return menuMapper.selectList(new LambdaQueryWrapper<Menu>().in(Menu::getId, ids));
  }

  @Override
  public long create(Menu menu) {
    menuMapper.insert(menu);
    return menu.getId();
  }

  @Override
  public void update(Menu menu) {
    menuMapper.updateById(menu);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void delete(Long id) {
    menuMapper.deleteById(id);
  }
}
