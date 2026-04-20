package com.xngl.web.auth;

import com.xngl.infrastructure.config.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION = "Authorization";
  private static final String BEARER = "Bearer ";
  private static final String ATTR_USER_ID = "userId";
  private static final String ATTR_USERNAME = "username";

  private final JwtUtils jwtUtils;

  public JwtAuthFilter(JwtUtils jwtUtils) {
    this.jwtUtils = jwtUtils;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return true;
    }
    String uri = request.getRequestURI() != null ? request.getRequestURI() : "";
    String servletPath = request.getServletPath() != null ? request.getServletPath() : "";
    String pathInfo = request.getPathInfo() != null ? request.getPathInfo() : "";
    String path = servletPath + pathInfo;
    if (path.isEmpty()) {
      path = uri;
    }
    List<String> skip =
        List.of(
            "/api/auth/login",
            "/api/auth/sso/exchange",
            "/api/mini/auth/send-sms-code",
            "/api/mini/auth/login",
            "/api/mini/auth/openid-login",
            "/api/health",
            "/swagger-ui",
            "/v3/api-docs");
    if (skip.stream().anyMatch(path::startsWith) || skip.stream().anyMatch(uri::startsWith)) {
      return true;
    }
    if ("POST".equalsIgnoreCase(request.getMethod()) && (uri.contains("login") || path.contains("login"))) {
      return true;
    }
    if ("POST".equalsIgnoreCase(request.getMethod()) && (uri.contains("mini") || path.contains("mini"))) {
      return true;
    }
    return uri.contains("swagger")
        || path.contains("swagger")
        || uri.contains("api-docs")
        || path.contains("api-docs");
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain) throws ServletException, IOException {
    String header = request.getHeader(AUTHORIZATION);
    if (header == null || !header.startsWith(BEARER)) {
      writeUnauthorized(response, "未登录或 token 缺失");
      return;
    }

    String token = header.substring(BEARER.length()).trim();
    if (!jwtUtils.validateToken(token)) {
      writeUnauthorized(response, "未登录或 token 无效");
      return;
    }

    var claims = jwtUtils.parseToken(token);
    request.setAttribute(ATTR_USER_ID, claims.get("userId", String.class));
    request.setAttribute(ATTR_USERNAME, claims.getSubject());

    // 设置租户上下文
    Long tenantId = claims.get("tenantId", Long.class);
    if (tenantId != null) {
      TenantContextHolder.setTenantId(tenantId);
    }

    // 设置角色上下文（用于超级管理员判断）
    String role = claims.get("role", String.class);
    if (role != null) {
      TenantContextHolder.setRole(role);
    }

    try {
      chain.doFilter(request, response);
    } finally {
      // 请求结束后清除租户上下文
      TenantContextHolder.clear();
    }
  }

  private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}");
  }
}
