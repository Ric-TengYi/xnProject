package com.xngl.manager.vehicle.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class VehicleInsurance {
    private Long id;
    private Long vehicleId;
    private String policyNo;
    private String insuranceType;
    private String insurerName;
    private BigDecimal coverageAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public void setId(Long id) {
        this.id = id;
    }
}
