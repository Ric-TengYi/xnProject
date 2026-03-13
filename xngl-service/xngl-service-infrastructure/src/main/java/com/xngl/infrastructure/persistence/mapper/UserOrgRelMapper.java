package com.xngl.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.infrastructure.persistence.entity.organization.UserOrgRel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserOrgRelMapper extends BaseMapper<UserOrgRel> {}
