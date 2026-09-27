package com.ticketstampede;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TicketStampedeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketStampedeApplication.class, args);
    }
}
