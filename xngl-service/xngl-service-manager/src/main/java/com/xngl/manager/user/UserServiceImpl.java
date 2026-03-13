package com.xngl.manager.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.organization.UserOrgRel;
import com.xngl.infrastructure.persistence.entity.organization.UserRoleRel;
import com.xngl.infrastructure.persistence.mapper.UserMapper;
import com.xngl.infrastructure.persistence.mapper.UserOrgRelMapper;
import com.xngl.infrastructure.persistence.mapper.UserRoleRelMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {

  private final UserMapper userMapper;
  private final UserRoleRelMapper userRoleRelMapper;
  private final UserOrgRelMapper userOrgRelMapper;
  private final PasswordEncoder passwordEncoder;

  public UserServiceImpl(
      UserMapper userMapper,
      UserRoleRelMapper userRoleRelMapper,
      UserOrgRelMapper userOrgRelMapper,
      PasswordEncoder passwordEncoder) {
    this.userMapper = userMapper;
    this.userRoleRelMapper = userRoleRelMapper;
    this.userOrgRelMapper = userOrgRelMapper;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public User getById(Long id) {
    return userMapper.selectById(id);
  }

  @Override
  public IPage<User> page(
      Long tenantId, String keyword, Long orgId, String status, int pageNo, int pageSize) {
    LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
    if (tenantId != null) q.eq(User::getTenantId, tenantId);
    if (StringUtils.hasText(keyword)) {
      q.and(
          w ->
              w.like(User::getUsername, keyword)
                  .or()
                  .like(User::getName, keyword)
                  .or()
                  .like(User::getMobile, keyword));
    }
    if (orgId != null) {
      List<Long> userIds =
          userOrgRelMapper
              .selectList(
                  new LambdaQueryWrapper<UserOrgRel>().eq(UserOrgRel::getOrgId, orgId))
              .stream()
              .map(UserOrgRel::getUserId)
              .distinct()
              .collect(Collectors.toList());
      if (userIds.isEmpty()) q.apply("1 = 0");
      else q.in(User::getId, userIds);
    }
    if (StringUtils.hasText(status)) q.eq(User::getStatus, status);
    return userMapper.selectPage(new Page<>(pageNo, pageSize), q);
  }

  @Override
  public long create(User user) {
    encodePasswordIfNeeded(user);
    userMapper.insert(user);
    return user.getId();
  }

  @Override
  public void update(User user) {
    encodePasswordIfNeeded(user);
    userMapper.updateById(user);
  }

  @Override
  public void updateStatus(Long id, String status) {
    User u = new User();
    u.setId(id);
    u.setStatus(status);
    userMapper.updateById(u);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void delete(Long id) {
    userRoleRelMapper.delete(new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, id));
    userOrgRelMapper.delete(new LambdaQueryWrapper<UserOrgRel>().eq(UserOrgRel::getUserId, id));
    userMapper.deleteById(id);
  }

  @Override
  public void resetPassword(Long id, String newPasswordEncrypted) {
    User u = new User();
    u.setId(id);
    u.setPasswordEncrypted(newPasswordEncrypted);
    encodePasswordIfNeeded(u);
    userMapper.updateById(u);
  }

  @Override
  public User getByUsername(String username) {
    return userMapper.selectOne(
        new LambdaQueryWrapper<User>().eq(User::getUsername, username).last("LIMIT 1"));
  }

  @Override
  public List<Long> listRoleIdsByUserId(Long userId) {
    return userRoleRelMapper
        .selectList(new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, userId))
        .stream()
        .map(UserRoleRel::getRoleId)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void updateRoles(Long userId, List<Long> roleIds) {
    User user = userMapper.selectById(userId);
    if (user == null) return;
    Long tenantId = user.getTenantId();
    userRoleRelMapper.delete(new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, userId));
    if (!CollectionUtils.isEmpty(roleIds)) {
      for (Long roleId : roleIds) {
        UserRoleRel rel = new UserRoleRel();
        rel.setTenantId(tenantId);
        rel.setUserId(userId);
        rel.setRoleId(roleId);
        userRoleRelMapper.insert(rel);
      }
    }
  }

  @Override
  public List<Long> listOrgIdsByUserId(Long userId) {
    return userOrgRelMapper
        .selectList(new LambdaQueryWrapper<UserOrgRel>().eq(UserOrgRel::getUserId, userId))
        .stream()
        .map(UserOrgRel::getOrgId)
        .collect(Collectors.toList());
  }

  @Override
  public Long getMainOrgIdByUserId(Long userId) {
    List<UserOrgRel> list =
        userOrgRelMapper.selectList(
            new LambdaQueryWrapper<UserOrgRel>()
                .eq(UserOrgRel::getUserId, userId)
                .eq(UserOrgRel::getIsMain, true));
    return list.isEmpty() ? null : list.get(0).getOrgId();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void updateOrgs(Long userId, Long mainOrgId, List<Long> orgIds) {
    User user = userMapper.selectById(userId);
    if (user == null) return;
    Long tenantId = user.getTenantId();
    LinkedHashSet<Long> effectiveOrgIds = new LinkedHashSet<>();
    if (!CollectionUtils.isEmpty(orgIds)) {
      effectiveOrgIds.addAll(orgIds);
    }
    if (mainOrgId != null) {
      effectiveOrgIds.add(mainOrgId);
    }

    userOrgRelMapper.delete(new LambdaQueryWrapper<UserOrgRel>().eq(UserOrgRel::getUserId, userId));
    if (!effectiveOrgIds.isEmpty()) {
      for (Long orgId : effectiveOrgIds) {
        UserOrgRel rel = new UserOrgRel();
        rel.setTenantId(tenantId);
        rel.setUserId(userId);
        rel.setOrgId(orgId);
        rel.setIsMain(mainOrgId != null && mainOrgId.equals(orgId) ? 1 : 0);
        userOrgRelMapper.insert(rel);
      }
    }

    User u = new User();
    u.setId(userId);
    u.setMainOrgId(mainOrgId);
    userMapper.updateById(u);
  }

  private void encodePasswordIfNeeded(User user) {
    if (user == null || !StringUtils.hasText(user.getPasswordHash())) {
      return;
    }
    String passwordHash = normalizeHash(user.getPasswordHash());
    if (passwordHash.startsWith("$2")) {
      user.setPasswordEncrypted(passwordHash);
      return;
    }
    user.setPasswordEncrypted(passwordEncoder.encode(passwordHash));
  }

  private String normalizeHash(String passwordHash) {
    if (passwordHash.startsWith("{bcrypt}")) {
      return passwordHash.substring("{bcrypt}".length());
    }
    return passwordHash;
  }
}
