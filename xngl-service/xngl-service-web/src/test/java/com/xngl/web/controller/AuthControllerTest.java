package com.xngl.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.mapper.SsoLoginTicketMapper;
import com.xngl.manager.auth.AuthService;
import com.xngl.manager.log.LoginLogService;
import com.xngl.manager.user.UserService;
import com.xngl.web.auth.JwtProperties;
import com.xngl.web.auth.JwtUtils;
import com.xngl.web.auth.dto.LoginRequest;
import com.xngl.web.auth.dto.LoginResponse;
import com.xngl.web.dto.ApiResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock
  private AuthService authService;

  @Mock
  private UserService userService;

  @Mock
  private LoginLogService loginLogService;

  @Mock
  private SsoLoginTicketMapper ssoLoginTicketMapper;

  @Test
  void loginShouldEmbedTenantIdIntoJwtToken() {
    JwtProperties jwtProperties = new JwtProperties();
    jwtProperties.setSecret("xngl-test-secret-with-32-plus-chars");
    JwtUtils jwtUtils = new JwtUtils(jwtProperties);
    AuthController controller =
        new AuthController(
            jwtUtils, authService, userService, loginLogService, ssoLoginTicketMapper);
    LoginRequest requestBody = new LoginRequest();
    requestBody.setTenantId("1");
    requestBody.setUsername("admin");
    requestBody.setPassword("admin");
    MockHttpServletRequest request = new MockHttpServletRequest();
    User user = buildUser(6L, 1L, 2L, "admin", "Local Admin", "TENANT_ADMIN");

    when(authService.getByTenantAndUsername(1L, "admin")).thenReturn(user);
    when(authService.checkPassword(user, "admin")).thenReturn(true);

    ApiResult<LoginResponse> result = controller.login(requestBody, request);

    String token = result.getData().getToken();
    assertThat(jwtUtils.parseToken(token).get("tenantId", Long.class)).isEqualTo(1L);
    verify(userService).updateLastLoginTime(6L);
  }

  private User buildUser(
      Long id, Long tenantId, Long mainOrgId, String username, String name, String userType) {
    User user = new User();
    user.setId(id);
    user.setTenantId(tenantId);
    user.setMainOrgId(mainOrgId);
    user.setUsername(username);
    user.setName(name);
    user.setUserType(userType);
    user.setStatus("ENABLED");
    return user;
  }
}
