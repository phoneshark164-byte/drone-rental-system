package com.drone.rental.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 无人机租赁系统启动类
 */
@SpringBootApplication(scanBasePackages = "com.drone.rental")
@MapperScan("com.drone.rental.dao.mapper")
public class DroneRentalApplication {

    public static void main(String[] args) {
        SpringApplication.run(DroneRentalApplication.class, args);
    }
}
