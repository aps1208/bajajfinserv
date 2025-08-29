package com.aman22bce8754.bhfl;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.aman22bce8754.bhfl.service.ChallengeService;

import lombok.RequiredArgsConstructor;

@SpringBootApplication
@RequiredArgsConstructor
public class BhflApplication {

    private final ChallengeService challengeService;

    public static void main(String[] args) {
        SpringApplication.run(BhflApplication.class, args);
    }

    @Bean
    CommandLineRunner runOnStartup() {
        return args -> challengeService.run();
    }
}