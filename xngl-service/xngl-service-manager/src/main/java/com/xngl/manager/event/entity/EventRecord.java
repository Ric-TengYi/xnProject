package com.xngl.manager.event.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventRecord {
    private Long id;
    private String eventType;
    private String eventSource;
    private Long vehicleId;
    private Long driverId;
    private String location;
    private String description;
    private String level;
    private String status;
    private LocalDateTime eventTime;
    private LocalDateTime createTime;
}