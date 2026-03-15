package com.xngl.manager.infoquery.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class InfoQuery {
    private Long id;
    private String queryType;
    private String keyword;
    private String result;
    private String status;
    private LocalDateTime createTime;
}
