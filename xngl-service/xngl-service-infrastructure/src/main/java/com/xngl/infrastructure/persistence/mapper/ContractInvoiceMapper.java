package com.xngl.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.infrastructure.persistence.entity.contract.ContractInvoice;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ContractInvoiceMapper extends BaseMapper<ContractInvoice> {}