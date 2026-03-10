package com.xngl.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.xngl.infrastructure.persistence.mapper")
public class MybatisPlusConfig {}
