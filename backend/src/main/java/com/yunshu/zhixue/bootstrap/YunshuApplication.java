package com.yunshu.zhixue.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication(scanBasePackages = "com.yunshu.zhixue")
@MapperScan(basePackages = "com.yunshu.zhixue.modules", annotationClass = Mapper.class)
public class YunshuApplication {
    public static void main(String[] args) {
        SpringApplication.run(YunshuApplication.class, args);
    }
}
