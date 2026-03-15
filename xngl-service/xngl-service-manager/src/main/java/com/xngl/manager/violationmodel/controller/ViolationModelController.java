package com.xngl.manager.violationmodel.controller;
import com.xngl.manager.violationmodel.entity.ViolationModel;
import com.xngl.manager.violationmodel.mapper.ViolationModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/violation-models")
public class ViolationModelController {
    @Autowired
    private ViolationModelMapper mapper;
    @GetMapping
    public List<ViolationModel> list() { return mapper.selectList(null); }
    @GetMapping("/{id}")
    public ViolationModel getById(@PathVariable Long id) { return mapper.selectById(id); }
}
