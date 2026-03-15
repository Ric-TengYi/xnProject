package com.xngl.manager.fleet.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FleetVehicleRel {
    private Long id;
    private Long fleetId;
    private Long vehicleId;
    private String joinDate;
    private String leaveDate;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}