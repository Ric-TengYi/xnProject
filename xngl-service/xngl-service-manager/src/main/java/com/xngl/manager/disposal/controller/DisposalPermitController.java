package com.xngl.manager.disposal.controller;

import com.xngl.manager.disposal.entity.DisposalPermit;
import com.xngl.manager.disposal.mapper.DisposalPermitMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/_legacy/disposal-permits")
public class DisposalPermitController {

    @Autowired
    private DisposalPermitMapper mapper;

    @GetMapping
    public List<DisposalPermit> list() {
        return mapper.selectList(null);
    }

    @GetMapping("/{id}")
    public DisposalPermit getById(@PathVariable Long id) {
        return mapper.selectById(id);
    }

    @PostMapping
    public int create(@RequestBody DisposalPermit permit) {
        return mapper.insert(permit);
    }

    @PutMapping("/{id}")
    public int update(@PathVariable Long id, @RequestBody DisposalPermit permit) {
        permit.setId(id);
        return mapper.updateById(permit);
    }

    @DeleteMapping("/{id}")
    public int delete(@PathVariable Long id) {
        return mapper.deleteById(id);
    }
}
