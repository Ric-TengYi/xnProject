package com.xngl.manager.violationmodel.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class ViolationModel {
    private Long id;
    private String modelName;
    private String modelType;
    private String thresholds;
    private String status;
    private LocalDateTime createTime;
}
