package com.xngl.manager.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.mapper.UserMapper;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthServiceImpl implements AuthService {

  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
    this.userMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public User getByUsername(String username) {
    List<User> users =
        userMapper.selectList(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username).last("LIMIT 2"));
    return users.size() == 1 ? users.get(0) : null;
  }

  @Override
  public User getByTenantAndUsername(Long tenantId, String username) {
    if (tenantId == null) return getByUsername(username);
    List<User> users =
        userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .eq(User::getTenantId, tenantId)
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
    return users.isEmpty() ? null : users.get(0);
  }

  @Override
  public boolean checkPassword(User user, String rawPassword) {
    if (user == null || rawPassword == null) return false;
    String hash = StringUtils.hasText(user.getPasswordHash()) ? user.getPasswordHash() : user.getPassword();
    if (!StringUtils.hasText(hash)) return false;

    String normalizedHash = normalizeHash(hash);
    if (normalizedHash.startsWith("$2")) {
      return passwordEncoder.matches(rawPassword, normalizedHash);
    }
    return normalizedHash.equals(rawPassword);
  }

  private String normalizeHash(String hash) {
    if (hash.startsWith("{bcrypt}")) {
      return hash.substring("{bcrypt}".length());
    }
    return hash;
  }
}
