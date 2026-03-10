package com.xngl.web.controller;

import com.xngl.web.dto.ApiResult;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

  @GetMapping("/dictionaries")
  public ApiResult<List<?>> dictionaries() {
    return ApiResult.ok(List.of());
  }

  @GetMapping("/logs")
  public ApiResult<List<?>> logs() {
    return ApiResult.ok(List.of());
  }
}
