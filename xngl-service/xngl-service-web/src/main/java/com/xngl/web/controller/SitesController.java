package com.xngl.web.controller;

import com.xngl.web.dto.ApiResult;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sites")
public class SitesController {

  @GetMapping
  public ApiResult<List<?>> list() {
    return ApiResult.ok(List.of());
  }

  @GetMapping("/{id}")
  public ApiResult<?> get(@PathVariable Long id) {
    return ApiResult.ok(null);
  }

  @PostMapping
  public ApiResult<?> create(@RequestBody Object body) {
    return ApiResult.ok(null);
  }
}
