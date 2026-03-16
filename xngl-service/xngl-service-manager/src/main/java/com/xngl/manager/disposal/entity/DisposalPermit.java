package com.xngl.manager.disposal.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DisposalPermit {
    private Long id;
    private String permitNo;
    private Long projectId;
    private Long siteId;
    private String vehicleNo;
    private LocalDate issueDate;
    private LocalDate expireDate;
    private BigDecimal approvedVolume;
    private BigDecimal usedVolume;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}