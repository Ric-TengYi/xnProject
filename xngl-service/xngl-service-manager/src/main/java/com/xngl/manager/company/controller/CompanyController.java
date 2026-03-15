package com.xngl.manager.company.controller;

import com.xngl.manager.company.entity.Company;
import com.xngl.manager.company.mapper.CompanyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    @Autowired
    private CompanyMapper mapper;

    @GetMapping
    public List<Company> list() {
        return mapper.selectList(null);
    }

    @GetMapping("/{id}")
    public Company getById(@PathVariable Long id) {
        return mapper.selectById(id);
    }

    @PostMapping
    public int create(@RequestBody Company company) {
        return mapper.insert(company);
    }

    @PutMapping("/{id}")
    public int update(@PathVariable Long id, @RequestBody Company company) {
        company.setId(id);
        return mapper.updateById(company);
    }

    @DeleteMapping("/{id}")
    public int delete(@PathVariable Long id) {
        return mapper.deleteById(id);
    }
}