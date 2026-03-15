package com.xngl.manager.statistics.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StatisticsReport {
    private Long id;
    private String reportType;
    private String reportName;
    private LocalDate statDate;
    private String periodStart;
    private String periodEnd;
    private BigDecimal totalAmount;
    private Integer totalCount;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}