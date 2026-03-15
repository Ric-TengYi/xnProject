package com.xngl.manager.miniprogram.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MiniProgramUser {
    private Long id;
    private String openid;
    private String nickname;
    private String phone;
    private String role;
    private String status;
    private LocalDateTime createTime;
}
