package com.xngl.manager.sysparam.controller;
import com.xngl.manager.sysparam.entity.SysParam;
import com.xngl.manager.sysparam.mapper.SysParamMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/sys-params")
public class SysParamController {
    @Autowired
    private SysParamMapper mapper;
    @GetMapping
    public List<SysParam> list() { return mapper.selectList(null); }
    @GetMapping("/{id}")
    public SysParam getById(@PathVariable Long id) { return mapper.selectById(id); }
}
