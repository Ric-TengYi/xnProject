package com.xngl.manager.safety.controller;

import com.xngl.manager.safety.entity.SafetyViolation;
import com.xngl.manager.safety.mapper.SafetyViolationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/safety-violations")
public class SafetyViolationController {

    @Autowired
    private SafetyViolationMapper mapper;

    @GetMapping
    public List<SafetyViolation> list() {
        return mapper.selectList(null);
    }

    @GetMapping("/{id}")
    public SafetyViolation getById(@PathVariable Long id) {
        return mapper.selectById(id);
    }

    @PostMapping
    public int create(@RequestBody SafetyViolation violation) {
        return mapper.insert(violation);
    }

    @PutMapping("/{id}")
    public int update(@PathVariable Long id, @RequestBody SafetyViolation violation) {
        violation.setId(id);
        return mapper.updateById(violation);
    }

    @DeleteMapping("/{id}")
    public int delete(@PathVariable Long id) {
        return mapper.deleteById(id);
    }
}