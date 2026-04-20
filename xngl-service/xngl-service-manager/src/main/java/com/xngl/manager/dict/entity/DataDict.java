package com.xngl.manager.dict.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_data_dict")
public class DataDict {
    private Long id;
    private Long tenantId;
    private String dictType;
    private String dictCode;
    private String dictLabel;
    private String dictValue;
    private Integer sort;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
