package com.xngl.manager.contract.controller;

import com.xngl.manager.contract.entity.ContractReceipt;
import com.xngl.manager.contract.mapper.ContractReceiptMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("managerContractReceiptController")
@RequestMapping("/api/contract-receipts")
@ConditionalOnProperty(name = "app.manager-legacy.enabled", havingValue = "true")
public class ContractReceiptController {

    @Autowired
    private ContractReceiptMapper mapper;

    @GetMapping
    public List<ContractReceipt> list() {
        return mapper.selectList(null);
    }

    @GetMapping("/{id}")
    public ContractReceipt getById(@PathVariable Long id) {
        return mapper.selectById(id);
    }

    @PostMapping
    public int create(@RequestBody ContractReceipt receipt) {
        return mapper.insert(receipt);
    }

    @PutMapping("/{id}")
    public int update(@PathVariable Long id, @RequestBody ContractReceipt receipt) {
        receipt.setId(id);
        return mapper.updateById(receipt);
    }

    @DeleteMapping("/{id}")
    public int delete(@PathVariable Long id) {
        return mapper.deleteById(id);
    }
}