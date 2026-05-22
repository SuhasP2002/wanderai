package com.wanderai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WanderAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(WanderAiApplication.class, args);
    }
}
