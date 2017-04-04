package org.benetech.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
@Profile("default")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}