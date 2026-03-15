package com.xngl.manager.syswarning.controller;

import com.xngl.manager.syswarning.entity.SystemWarning;
import com.xngl.manager.syswarning.mapper.SystemWarningMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/system-warnings")
public class SystemWarningController {
    @Autowired
    private SystemWarningMapper mapper;

    @GetMapping
    public List<SystemWarning> list() { return mapper.selectList(null); }

    @GetMapping("/{id}")
    public SystemWarning getById(@PathVariable Long id) { return mapper.selectById(id); }

    @PostMapping
    public int create(@RequestBody SystemWarning warning) { return mapper.insert(warning); }
}
