package com.financebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinancebotApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinancebotApplication.class, args);
	}

}
