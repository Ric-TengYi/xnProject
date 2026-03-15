package com.xngl.manager.dict.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class DataDict {
    private Long id;
    private String dictType;
    private String dictCode;
    private String dictLabel;
    private String dictValue;
    private Integer sort;
    private String status;
    private LocalDateTime createTime;
}
