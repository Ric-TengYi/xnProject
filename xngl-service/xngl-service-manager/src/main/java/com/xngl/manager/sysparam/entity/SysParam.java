package com.xngl.manager.sysparam.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class SysParam {
    private Long id;
    private String paramKey;
    private String paramValue;
    private String paramType;
    private String remark;
    private LocalDateTime createTime;
}
