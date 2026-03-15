package com.xngl.manager.event.controller;

import com.xngl.manager.event.entity.EventRecord;
import com.xngl.manager.event.mapper.EventRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/event-records")
public class EventRecordController {
    @Autowired
    private EventRecordMapper mapper;

    @GetMapping
    public List<EventRecord> list() { return mapper.selectList(null); }

    @GetMapping("/{id}")
    public EventRecord getById(@PathVariable Long id) { return mapper.selectById(id); }

    @PostMapping
    public int create(@RequestBody EventRecord record) { return mapper.insert(record); }

    @PutMapping("/{id}")
    public int update(@PathVariable Long id, @RequestBody EventRecord record) {
        record.setId(id);
        return mapper.updateById(record);
    }
}