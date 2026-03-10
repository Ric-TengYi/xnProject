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
    String path = request.getRequestURI();
    List<String> skip = List.of("/api/auth/login", "/api/health", "/swagger-ui", "/v3/api-docs");
    return skip.stream().anyMatch(path::startsWith);
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain) throws ServletException, IOException {
    String header = request.getHeader(AUTHORIZATION);
    if (header != null && header.startsWith(BEARER)) {
      String token = header.substring(BEARER.length()).trim();
      if (jwtUtils.validateToken(token)) {
        var claims = jwtUtils.parseToken(token);
        request.setAttribute(ATTR_USER_ID, claims.get("userId", String.class));
        request.setAttribute(ATTR_USERNAME, claims.getSubject());
      }
    }
    chain.doFilter(request, response);
  }
}
