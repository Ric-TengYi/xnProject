package com.xngl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.xngl")
public class XnglServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(XnglServiceApplication.class, args);
  }
}
