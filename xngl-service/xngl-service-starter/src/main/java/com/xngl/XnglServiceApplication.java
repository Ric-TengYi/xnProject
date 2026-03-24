package com.xngl;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.xngl")
@EnableScheduling
@MapperScan({"com.xngl.infrastructure.persistence.mapper", "com.xngl.manager.**.mapper"})
public class XnglServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(XnglServiceApplication.class, args);
  }
}
