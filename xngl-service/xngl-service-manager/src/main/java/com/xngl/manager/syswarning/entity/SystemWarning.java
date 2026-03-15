package com.xngl.manager.syswarning.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SystemWarning {
    private Long id;
    private String warningType;
    private String warningLevel;
    private String title;
    private String content;
    private Long relatedId;
    private String status;
    private LocalDateTime createTime;
}