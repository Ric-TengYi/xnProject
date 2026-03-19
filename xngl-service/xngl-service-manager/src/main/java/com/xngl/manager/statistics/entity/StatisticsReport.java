package com.xngl.manager.statistics.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

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

    public void setId(Long id) {
        this.id = id;
    }
}
