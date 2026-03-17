package com.xngl.manager.site.controller;

import com.xngl.manager.site.entity.SiteSettlement;
import com.xngl.manager.site.mapper.SiteSettlementMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("managerSiteSettlementController")
@RequestMapping("/api/site-settlements")
@ConditionalOnProperty(name = "app.manager-legacy.enabled", havingValue = "true")
public class SiteSettlementController {

    @Autowired
    private SiteSettlementMapper mapper;

    @GetMapping
    public List<SiteSettlement> list() {
        return mapper.selectList(null);
    }

    @GetMapping("/{id}")
    public SiteSettlement getById(@PathVariable Long id) {
        return mapper.selectById(id);
    }

    @PostMapping
    public int create(@RequestBody SiteSettlement settlement) {
        return mapper.insert(settlement);
    }

    @PutMapping("/{id}")
    public int update(@PathVariable Long id, @RequestBody SiteSettlement settlement) {
        settlement.setId(id);
        return mapper.updateById(settlement);
    }

    @DeleteMapping("/{id}")
    public int delete(@PathVariable Long id) {
        return mapper.deleteById(id);
    }
}