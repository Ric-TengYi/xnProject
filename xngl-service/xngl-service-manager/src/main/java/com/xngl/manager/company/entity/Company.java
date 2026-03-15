package com.xngl.manager.company.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Company {
    private Long id;
    private String companyName;
    private String companyCode;
    private String companyType;
    private String linkman;
    private String phone;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}