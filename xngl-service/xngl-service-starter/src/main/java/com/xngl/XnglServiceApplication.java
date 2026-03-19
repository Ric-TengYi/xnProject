package com.xngl;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.xngl")
@MapperScan({"com.xngl.infrastructure.persistence.mapper", "com.xngl.manager.**.mapper", "com.xngl.miniprogram.mapper"})
public class XnglServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(XnglServiceApplication.class, args);
  }
}
