package com.xngl.manager.role.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class Role {
    private Long id;
    private String roleName;
    private String roleCode;
    private String description;
    private String status;
    private LocalDateTime createTime;
}
