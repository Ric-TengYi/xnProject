package com.xngl.manager.warning.controller;

import com.xngl.manager.warning.entity.WarningRule;
import com.xngl.manager.warning.mapper.WarningRuleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warning-rules")
public class WarningRuleController {

    @Autowired
    private WarningRuleMapper mapper;

    @GetMapping
    public List<WarningRule> list() {
        return mapper.selectList(null);
    }

    @GetMapping("/{id}")
    public WarningRule getById(@PathVariable Long id) {
        return mapper.selectById(id);
    }

    @PostMapping
    public int create(@RequestBody WarningRule rule) {
        return mapper.insert(rule);
    }

    @PutMapping("/{id}")
    public int update(@PathVariable Long id, @RequestBody WarningRule rule) {
        rule.setId(id);
        return mapper.updateById(rule);
    }

    @DeleteMapping("/{id}")
    public int delete(@PathVariable Long id) {
        return mapper.deleteById(id);
    }
}