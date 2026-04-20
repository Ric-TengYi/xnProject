package com.xngl.manager.vehicle.controller;

import com.xngl.manager.vehicle.entity.VehicleInsurance;
import com.xngl.manager.vehicle.mapper.VehicleInsuranceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/_legacy/vehicle-insurances")
public class VehicleInsuranceController {

    @Autowired
    private VehicleInsuranceMapper mapper;

    @GetMapping
    public List<VehicleInsurance> list() {
        return mapper.selectList(null);
    }

    @GetMapping("/{id}")
    public VehicleInsurance getById(@PathVariable Long id) {
        return mapper.selectById(id);
    }

    @PostMapping
    public int create(@RequestBody VehicleInsurance insurance) {
        return mapper.insert(insurance);
    }

    @PutMapping("/{id}")
    public int update(@PathVariable Long id, @RequestBody VehicleInsurance insurance) {
        insurance.setId(id);
        return mapper.updateById(insurance);
    }

    @DeleteMapping("/{id}")
    public int delete(@PathVariable Long id) {
        return mapper.deleteById(id);
    }
}