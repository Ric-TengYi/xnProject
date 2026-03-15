package com.xngl.manager.integration.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PlatformApiLog {
    private Long id;
    private String platformName;
    private String apiName;
    private String requestUrl;
    private String requestBody;
    private String responseBody;
    private Integer statusCode;
    private Long costTime;
    private LocalDateTime createTime;
}