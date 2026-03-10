package com.xngl.manager.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

  private final UserMapper userMapper;

  public AuthServiceImpl(UserMapper userMapper) {
    this.userMapper = userMapper;
  }

  @Override
  public User getByUsername(String username) {
    return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
  }

  @Override
  public boolean checkPassword(User user, String rawPassword) {
    if (user == null) return false;
    return rawPassword != null && rawPassword.equals(user.getPassword());
  }
}
