package com.pg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PgAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(PgAppApplication.class, args);
    }
}
