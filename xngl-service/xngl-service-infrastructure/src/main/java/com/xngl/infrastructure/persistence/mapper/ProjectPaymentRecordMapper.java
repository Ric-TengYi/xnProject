package com.xngl.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xngl.infrastructure.persistence.entity.project.ProjectPaymentRecord;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProjectPaymentRecordMapper extends BaseMapper<ProjectPaymentRecord> {

  @Select(
      """
      SELECT
        COALESCE(SUM(CASE WHEN status <> 'CANCELLED' THEN amount ELSE 0 END), 0) AS paidAmount,
        MAX(CASE WHEN status <> 'CANCELLED' THEN payment_date ELSE NULL END) AS lastPaymentDate
      FROM biz_project_payment_record
      WHERE tenant_id = #{tenantId}
        AND project_id = #{projectId}
        AND deleted = 0
      """)
  Map<String, Object> selectPaymentAggregate(
      @Param("tenantId") Long tenantId, @Param("projectId") Long projectId);
}
