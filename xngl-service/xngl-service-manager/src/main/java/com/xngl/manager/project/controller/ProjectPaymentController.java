package com.xngl.manager.project.controller;

import com.xngl.manager.project.entity.ProjectPaymentRecord;
import com.xngl.manager.project.mapper.ProjectPaymentRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project-payments")
public class ProjectPaymentController {

    @Autowired
    private ProjectPaymentRecordMapper mapper;

    @GetMapping
    public List<ProjectPaymentRecord> list() {
        return mapper.selectList(null);
    }

    @GetMapping("/{id}")
    public ProjectPaymentRecord getById(@PathVariable Long id) {
        return mapper.selectById(id);
    }

    @PostMapping
    public int create(@RequestBody ProjectPaymentRecord record) {
        return mapper.insert(record);
    }

    @PutMapping("/{id}")
    public int update(@PathVariable Long id, @RequestBody ProjectPaymentRecord record) {
        record.setId(id);
        return mapper.updateById(record);
    }

    @DeleteMapping("/{id}")
    public int delete(@PathVariable Long id) {
        return mapper.deleteById(id);
    }
}