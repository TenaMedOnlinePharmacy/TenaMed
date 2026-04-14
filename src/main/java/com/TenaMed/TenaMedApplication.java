package com.TenaMed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TenaMedApplication {

	public static void main(String[] args) {
		SpringApplication.run(TenaMedApplication.class, args);
	}

}
