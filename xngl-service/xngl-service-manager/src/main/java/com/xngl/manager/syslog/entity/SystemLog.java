package com.xngl.manager.syslog.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SystemLog {
    private Long id;
    private String module;
    private String operation;
    private String operator;
    private String ip;
    private String method;
    private String params;
    private String result;
    private LocalDateTime createTime;
}