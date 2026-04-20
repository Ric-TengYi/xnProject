package com.xngl.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.infrastructure.persistence.entity.system.SsoLoginTicket;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SsoLoginTicketMapper extends BaseMapper<SsoLoginTicket> {}
