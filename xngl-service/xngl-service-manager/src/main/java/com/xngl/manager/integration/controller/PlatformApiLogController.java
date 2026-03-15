package com.xngl.manager.integration.controller;

import com.xngl.manager.integration.entity.PlatformApiLog;
import com.xngl.manager.integration.mapper.PlatformApiLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform-logs")
public class PlatformApiLogController {

    @Autowired
    private PlatformApiLogMapper mapper;

    @GetMapping
    public List<PlatformApiLog> list() {
        return mapper.selectList(null);
    }

    @GetMapping("/{id}")
    public PlatformApiLog getById(@PathVariable Long id) {
        return mapper.selectById(id);
    }

    @PostMapping
    public int create(@RequestBody PlatformApiLog log) {
        return mapper.insert(log);
    }
}