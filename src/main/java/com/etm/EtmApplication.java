package com.etm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 企业培训管理系统 启动类
 */
@SpringBootApplication
@MapperScan("com.etm.mapper")
public class EtmApplication {

    public static void main(String[] args) {
        SpringApplication.run(EtmApplication.class, args);
    }
}
