package com.xngl.web.auth;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

  private static final Logger log = LoggerFactory.getLogger(JwtProperties.class);
  private static final String DEFAULT_SECRET = "xngl-default-secret-change-in-production";

  @Value("${spring.profiles.active:}")
  private String activeProfile;

  private String secret = DEFAULT_SECRET;
  private long expirationMs = 86400000L; // 24h

  @PostConstruct
  public void validate() {
    if (DEFAULT_SECRET.equals(secret) || secret == null || secret.isBlank() || secret.length() < 32) {
      if ("local".equalsIgnoreCase(activeProfile) || "dev".equalsIgnoreCase(activeProfile)) {
        log.warn("【安全警告】JWT secret 使用默认值，仅限本地/开发环境使用，生产环境必须更换！");
      } else {
        throw new IllegalStateException(
            "请在配置文件中设置 app.jwt.secret，长度至少 32 位，且不得使用默认值");
      }
    }
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public long getExpirationMs() {
    return expirationMs;
  }

  public void setExpirationMs(long expirationMs) {
    this.expirationMs = expirationMs;
  }
}
