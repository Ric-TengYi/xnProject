package com.xngl.manager.org.controller;
import com.xngl.manager.org.entity.Organization;
import com.xngl.manager.org.mapper.OrganizationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {
    @Autowired
    private OrganizationMapper mapper;
    @GetMapping
    public List<Organization> list() { return mapper.selectList(null); }
    @GetMapping("/{id}")
    public Organization getById(@PathVariable Long id) { return mapper.selectById(id); }
}
