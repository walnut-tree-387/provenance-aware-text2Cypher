package com.example.text2cypher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Text2cypherApplication {

	public static void main(String[] args) {
		SpringApplication.run(Text2cypherApplication.class, args);
	}
}
