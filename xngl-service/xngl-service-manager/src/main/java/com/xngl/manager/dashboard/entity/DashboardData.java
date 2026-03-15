package com.xngl.manager.dashboard.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DashboardData {
    private Long id;
    private String dashboardType;
    private String dataKey;
    private BigDecimal dataValue;
    private String unit;
    private LocalDateTime updateTime;
}