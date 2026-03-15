package com.xngl.manager.role.controller;
import com.xngl.manager.role.entity.Role;
import com.xngl.manager.role.mapper.RoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/roles")
public class RoleController {
    @Autowired
    private RoleMapper mapper;
    @GetMapping
    public List<Role> list() { return mapper.selectList(null); }
    @GetMapping("/{id}")
    public Role getById(@PathVariable Long id) { return mapper.selectById(id); }
}
