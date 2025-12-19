package com.example.coopachat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // permet d'activer le scheduling (planification des tâches)
public class CoopachatApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoopachatApplication.class, args);
		System.out.println("✨Coopachat Application Started✨");
	}

}
