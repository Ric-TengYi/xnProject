package com.xngl.manager.warning.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WarningRule {
    private Long id;
    private String ruleName;
    private String warningType;
    private String condition;
    private String threshold;
    private String level;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}