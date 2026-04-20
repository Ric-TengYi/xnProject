package com.xngl.manager.approval.controller;
import com.xngl.manager.approval.entity.ApprovalConfig;
import com.xngl.manager.approval.mapper.ApprovalConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/internal/approval-configs")
public class ApprovalConfigController {
    @Autowired
    private ApprovalConfigMapper mapper;
    @GetMapping
    public List<ApprovalConfig> list() { return mapper.selectList(null); }
    @GetMapping("/{id}")
    public ApprovalConfig getById(@PathVariable Long id) { return mapper.selectById(id); }
}
