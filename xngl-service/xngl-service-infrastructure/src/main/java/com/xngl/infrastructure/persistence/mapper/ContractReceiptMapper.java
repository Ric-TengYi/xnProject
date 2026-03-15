package com.xngl.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.infrastructure.persistence.entity.contract.ContractReceipt;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/** 合同入账流水 Mapper。 */
public interface ContractReceiptMapper extends BaseMapper<ContractReceipt> {}
