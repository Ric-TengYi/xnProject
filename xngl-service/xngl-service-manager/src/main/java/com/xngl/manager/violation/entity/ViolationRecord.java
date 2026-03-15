package com.xngl.manager.violation.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ViolationRecord {
    private Long id;
    private String vehicleNo;
    private String violationType;
    private String violationTime;
    private String location;
    private String description;
    private String penalty;
    private String status;
    private LocalDateTime createTime;
}
