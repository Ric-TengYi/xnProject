package com.xngl.manager.miniprogram.controller;

import com.xngl.manager.miniprogram.entity.MiniProgramUser;
import com.xngl.manager.miniprogram.mapper.MiniProgramUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/miniprogram-users")
public class MiniProgramUserController {
    @Autowired
    private MiniProgramUserMapper mapper;

    @GetMapping
    public List<MiniProgramUser> list() { return mapper.selectList(null); }
    @GetMapping("/{id}")
    public MiniProgramUser getById(@PathVariable Long id) { return mapper.selectById(id); }
}
