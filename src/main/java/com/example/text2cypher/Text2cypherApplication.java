package com.example.text2cypher;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.QueryConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class Text2cypherApplication {

	public static void main(String[] args) {
		SpringApplication.run(Text2cypherApplication.class, args);
	}
}
