package com.xngl.manager.dict.controller;
import com.xngl.manager.dict.entity.DataDict;
import com.xngl.manager.dict.mapper.DataDictMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/_legacy/data-dicts")
public class DataDictController {
    @Autowired
    private DataDictMapper mapper;
    @GetMapping
    public List<DataDict> list() { return mapper.selectList(null); }
    @GetMapping("/{id}")
    public DataDict getById(@PathVariable Long id) { return mapper.selectById(id); }
}
