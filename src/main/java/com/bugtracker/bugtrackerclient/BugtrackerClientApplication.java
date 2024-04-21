package com.bugtracker.bugtrackerclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BugtrackerClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(BugtrackerClientApplication.class, args);
    }

}
