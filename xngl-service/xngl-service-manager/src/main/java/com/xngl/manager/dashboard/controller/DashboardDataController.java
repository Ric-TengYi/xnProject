package com.xngl.manager.dashboard.controller;

import com.xngl.manager.dashboard.entity.DashboardData;
import com.xngl.manager.dashboard.mapper.DashboardDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardDataController {

    @Autowired
    private DashboardDataMapper mapper;

    @GetMapping
    public List<DashboardData> list() {
        return mapper.selectList(null);
    }

    @GetMapping("/{id}")
    public DashboardData getById(@PathVariable Long id) {
        return mapper.selectById(id);
    }
}