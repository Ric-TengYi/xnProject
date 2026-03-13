package com.xngl.infrastructure.persistence.entity.organization;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
public class Tenant extends BaseEntity {

  private String tenantCode;
  private String tenantName;
  private String tenantType;
  private String status;
  private String contactName;
  private String contactMobile;
  private String businessLicenseNo;
  private String address;
  private LocalDateTime expireTime;
  private String remark;
}
