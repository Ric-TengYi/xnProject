package com.xngl.manager.org.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class Organization {
    private Long id;
    private String orgName;
    private String orgCode;
    private Long parentId;
    private String orgLevel;
    private String status;
    private LocalDateTime createTime;
}
