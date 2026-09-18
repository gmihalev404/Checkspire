package com.example.checkspire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CheckspireApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckspireApplication.class, args);
    }

}
