package com.xngl.manager.violation.controller;

import com.xngl.manager.violation.entity.ViolationRecord;
import com.xngl.manager.violation.mapper.ViolationRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/violation-records")
public class ViolationRecordController {
    @Autowired
    private ViolationRecordMapper mapper;

    @GetMapping
    public List<ViolationRecord> list() { return mapper.selectList(null); }

    @GetMapping("/{id}")
    public ViolationRecord getById(@PathVariable Long id) { return mapper.selectById(id); }

    @PostMapping
    public int create(@RequestBody ViolationRecord record) { return mapper.insert(record); }
}
