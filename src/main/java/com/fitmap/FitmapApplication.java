package com.fitmap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FitmapApplication {
    public static void main(String[] args) {
        SpringApplication.run(FitmapApplication.class, args);
    }
}
