package com.xngl.web.controller;

import com.xngl.web.dto.ApiResult;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

  @GetMapping("/health")
  public ApiResult<Map<String, Object>> health() {
    return ApiResult.ok(Map.of("status", "UP", "service", "xngl-service"));
  }
}
