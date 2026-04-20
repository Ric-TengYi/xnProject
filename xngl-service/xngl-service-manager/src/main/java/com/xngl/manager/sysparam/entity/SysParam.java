package com.xngl.manager.sysparam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_param")
public class SysParam {
    private Long id;
    private Long tenantId;
    private String paramKey;
    private String paramName;
    private String paramValue;
    private String paramType;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
