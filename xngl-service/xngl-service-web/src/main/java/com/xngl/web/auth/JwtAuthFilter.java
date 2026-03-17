package com.xngl.web.auth;

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
    if (path.isEmpty()) path = uri;
    // 放行：平台登录、小程序登录、健康检查、Swagger
    if (uri.startsWith("/api/auth/login") || path.startsWith("/api/auth/login")) return true;
    if (uri.startsWith("/api/mini/auth/login") || path.startsWith("/api/mini/auth/login")) return true;
    if (uri.contains("/mini/auth/login") || path.contains("/mini/auth/login")) return true;
    if ("POST".equalsIgnoreCase(request.getMethod()) && (uri.contains("login") || path.contains("login"))) return true;
    if (uri.startsWith("/api/health") || path.startsWith("/api/health")) return true;
    if (uri.contains("swagger") || path.contains("swagger") || uri.contains("api-docs") || path.contains("api-docs")) return true;
    return false;
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
    chain.doFilter(request, response);
  }

  private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}");
  }
}
