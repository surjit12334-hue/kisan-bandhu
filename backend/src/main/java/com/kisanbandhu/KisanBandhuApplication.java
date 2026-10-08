package com.kisanbandhu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KisanBandhuApplication {

    public static void main(String[] args) {
        SpringApplication.run(KisanBandhuApplication.class, args);
    }
}
