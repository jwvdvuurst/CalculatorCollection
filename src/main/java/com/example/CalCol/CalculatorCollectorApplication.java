package com.example.CalCol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CalculatorCollectorApplication {

	public static void main(String[] args) {
		SpringApplication.run(CalculatorCollectorApplication.class, args);
	}

}
