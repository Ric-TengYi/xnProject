package com.xngl.web.controller;

import com.xngl.web.dto.ApiResult;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

  @GetMapping("/summary")
  public ApiResult<Map<String, Object>> summary() {
    return ApiResult.ok(Map.of(
        "projectCount", 0,
        "siteCount", 0,
        "vehicleCount", 0,
        "alertCount", 0));
  }
}
