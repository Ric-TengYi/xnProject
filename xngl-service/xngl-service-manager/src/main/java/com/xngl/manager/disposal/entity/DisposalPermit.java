package com.xngl.manager.disposal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("disposal_permit")
public class DisposalPermit {
    private Long id;
    private Long tenantId;
    private String permitNo;
    private String permitType;
    private Long projectId;
    private Long contractId;
    private Long siteId;
    private String vehicleNo;
    private LocalDate issueDate;
    private LocalDate expireDate;
    private BigDecimal approvedVolume;
    private BigDecimal usedVolume;
    private String status;
    private String bindStatus;
    private String sourcePlatform;
    private String externalRefNo;
    private LocalDateTime lastSyncTime;
    private String syncBatchNo;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
