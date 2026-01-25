package com.im.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.im.mapper")
public class MyBatisPlusConfig {
}
