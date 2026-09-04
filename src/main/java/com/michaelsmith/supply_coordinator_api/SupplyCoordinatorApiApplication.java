package com.michaelsmith.supply_coordinator_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SupplyCoordinatorApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SupplyCoordinatorApiApplication.class, args);
	}

}
