package com.xngl.manager.infoquery.controller;
import com.xngl.manager.infoquery.entity.InfoQuery;
import com.xngl.manager.infoquery.mapper.InfoQueryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/info-queries")
public class InfoQueryController {
    @Autowired
    private InfoQueryMapper mapper;
    @GetMapping
    public List<InfoQuery> list() { return mapper.selectList(null); }
    @GetMapping("/{id}")
    public InfoQuery getById(@PathVariable Long id) { return mapper.selectById(id); }
}
