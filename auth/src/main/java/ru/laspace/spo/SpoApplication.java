package ru.laspace.spo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SpoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpoApplication.class, args);
	}

}
