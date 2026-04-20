package com.xngl;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.xngl")
@EnableScheduling
@MapperScan({"com.xngl.infrastructure.persistence.mapper", "com.xngl.manager.**.mapper"})
@ComponentScan(
    basePackages = "com.xngl",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.xngl\\.manager\\..*\\.controller\\..*"))
public class XnglServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(XnglServiceApplication.class, args);
  }
}
