package com.xngl.manager.approval.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class ApprovalConfig {
    private Long id;
    private String configName;
    private String approvalType;
    private String approvers;
    private String conditions;
    private String status;
    private LocalDateTime createTime;
}
