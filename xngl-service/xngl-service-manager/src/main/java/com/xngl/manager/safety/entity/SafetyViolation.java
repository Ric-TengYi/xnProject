package com.xngl.manager.safety.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SafetyViolation {
    private Long id;
    private Long vehicleId;
    private Long driverId;
    private String violationType;
    private String violationDesc;
    private String location;
    private LocalDateTime violationTime;
    private String penalty;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}