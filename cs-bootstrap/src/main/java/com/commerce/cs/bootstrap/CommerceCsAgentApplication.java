package com.commerce.cs.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.commerce.cs")
public class CommerceCsAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommerceCsAgentApplication.class, args);
    }
}
