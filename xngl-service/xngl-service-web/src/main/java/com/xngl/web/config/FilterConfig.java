package com.xngl.web.config;

import org.springframework.context.annotation.Configuration;

/**
 * 原 JWT 过滤器通过 FilterRegistrationBean 仅对 /api/* 生效；
 * 改为依赖 JwtAuthFilter 自身 @Component + shouldNotFilter，避免与 URL 映射差异导致 /api/mini/auth/login 未放行。
 */
@Configuration
public class FilterConfig {
  // JwtAuthFilter 已为 @Component，由 OncePerRequestFilter 自动注册，无需此处再注册
}
