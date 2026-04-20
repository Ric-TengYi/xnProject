package com.xngl.web.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {

  private final JwtProperties props;

  public JwtUtils(JwtProperties props) {
    this.props = props;
  }

  public String createToken(String subject, String userId) {
    return createToken(subject, userId, null);
  }

  public String createToken(String subject, String userId, Long tenantId) {
    SecretKey key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    var builder =
        Jwts.builder()
            .setSubject(subject)
            .claim("userId", userId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + props.getExpirationMs()))
            .signWith(key);
    if (tenantId != null) {
      builder.claim("tenantId", tenantId);
    }
    return builder.compact();
  }

  public Claims parseToken(String token) {
    SecretKey key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  public boolean validateToken(String token) {
    try {
      parseToken(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
