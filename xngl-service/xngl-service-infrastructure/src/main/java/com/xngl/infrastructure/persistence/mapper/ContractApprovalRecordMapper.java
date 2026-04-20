package com.xngl.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.infrastructure.persistence.entity.contract.ContractApprovalRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ContractApprovalRecordMapper extends BaseMapper<ContractApprovalRecord> {}