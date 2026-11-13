package com.trustbank.loanapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration.class})
@ConfigurationPropertiesScan
public class LoanBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanBackendApplication.class, args);
    }
}
