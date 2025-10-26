package com.interview.seckill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 秒杀系统启动类
 * 
 * @author interview
 * @since 2024-01-01
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
@EnableScheduling
public class SeckillSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillSystemApplication.class, args);
    }
}
