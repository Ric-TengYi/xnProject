package com.xngl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication(scanBasePackages = "com.xngl")
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
