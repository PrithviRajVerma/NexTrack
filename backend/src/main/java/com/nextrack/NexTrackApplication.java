package com.nextrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NexTrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexTrackApplication.class, args);
    }
}
