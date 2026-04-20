package com.xngl.manager.warning.entity;

import java.time.LocalDateTime;
import lombok.Data;

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

    public void setId(Long id) {
        this.id = id;
    }
}
