package com.ven.predicktions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PredicktionApplication {

    public static void main(String[] args) {
        SpringApplication.run(PredicktionApplication.class, args);
    }
}