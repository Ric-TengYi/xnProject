package com.xngl.manager.message.controller;

import com.xngl.manager.message.entity.MessageRecord;
import com.xngl.manager.message.mapper.MessageRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageRecordController {

    @Autowired
    private MessageRecordMapper mapper;

    @GetMapping
    public List<MessageRecord> list() {
        return mapper.selectList(null);
    }

    @GetMapping("/{id}")
    public MessageRecord getById(@PathVariable Long id) {
        return mapper.selectById(id);
    }

    @PostMapping
    public int create(@RequestBody MessageRecord record) {
        return mapper.insert(record);
    }

    @DeleteMapping("/{id}")
    public int delete(@PathVariable Long id) {
        return mapper.deleteById(id);
    }
}