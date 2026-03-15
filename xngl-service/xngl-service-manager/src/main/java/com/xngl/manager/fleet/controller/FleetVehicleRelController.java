package com.xngl.manager.fleet.controller;

import com.xngl.manager.fleet.entity.FleetVehicleRel;
import com.xngl.manager.fleet.mapper.FleetVehicleRelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fleet-vehicles")
public class FleetVehicleRelController {

    @Autowired
    private FleetVehicleRelMapper mapper;

    @GetMapping
    public List<FleetVehicleRel> list() {
        return mapper.selectList(null);
    }

    @GetMapping("/{id}")
    public FleetVehicleRel getById(@PathVariable Long id) {
        return mapper.selectById(id);
    }

    @PostMapping
    public int create(@RequestBody FleetVehicleRel rel) {
        return mapper.insert(rel);
    }

    @PutMapping("/{id}")
    public int update(@PathVariable Long id, @RequestBody FleetVehicleRel rel) {
        rel.setId(id);
        return mapper.updateById(rel);
    }

    @DeleteMapping("/{id}")
    public int delete(@PathVariable Long id) {
        return mapper.deleteById(id);
    }
}