package com.xngl.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.infrastructure.persistence.entity.system.DataScopeRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DataScopeRuleMapper extends BaseMapper<DataScopeRule> {

  @Delete("DELETE FROM sys_data_scope_rule WHERE role_id = #{roleId}")
  void deletePhysicalByRoleId(@Param("roleId") Long roleId);
}
