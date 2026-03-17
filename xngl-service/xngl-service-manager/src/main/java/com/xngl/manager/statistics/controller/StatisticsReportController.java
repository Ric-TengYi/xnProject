package com.xngl.manager.statistics.controller;

import com.xngl.manager.statistics.entity.StatisticsReport;
import com.xngl.manager.statistics.mapper.StatisticsReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statistics-reports")
@ConditionalOnProperty(name = "app.statistics.enabled", havingValue = "true")
public class StatisticsReportController {

    @Autowired
    private StatisticsReportMapper mapper;

    @GetMapping
    public List<StatisticsReport> list() {
        return mapper.selectList(null);
    }

    @GetMapping("/{id}")
    public StatisticsReport getById(@PathVariable Long id) {
        return mapper.selectById(id);
    }

    @PostMapping
    public int create(@RequestBody StatisticsReport report) {
        return mapper.insert(report);
    }

    @PutMapping("/{id}")
    public int update(@PathVariable Long id, @RequestBody StatisticsReport report) {
        report.setId(id);
        return mapper.updateById(report);
    }

    @DeleteMapping("/{id}")
    public int delete(@PathVariable Long id) {
        return mapper.deleteById(id);
    }
}