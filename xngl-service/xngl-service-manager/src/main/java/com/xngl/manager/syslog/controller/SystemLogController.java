package com.xngl.manager.syslog.controller;

import com.xngl.manager.syslog.entity.SystemLog;
import com.xngl.manager.syslog.mapper.SystemLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/system-logs")
public class SystemLogController {
    @Autowired
    private SystemLogMapper mapper;

    @GetMapping
    public List<SystemLog> list() { return mapper.selectList(null); }

    @GetMapping("/{id}")
    public SystemLog getById(@PathVariable Long id) { return mapper.selectById(id); }

    @PostMapping
    public int create(@RequestBody SystemLog log) { return mapper.insert(log); }
}