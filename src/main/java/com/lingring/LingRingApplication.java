package com.lingring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LingRingApplication {

	public static void main(String[] args) {
		SpringApplication.run(LingRingApplication.class, args);
	}

}
