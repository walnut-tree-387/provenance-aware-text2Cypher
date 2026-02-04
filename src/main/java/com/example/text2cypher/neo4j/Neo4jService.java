package com.example.text2cypher.neo4j;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class Neo4jService implements AutoCloseable {
    @Value("${neo4j.db}") String db;
    private final Driver driver;
    public Neo4jService(
            @Value("${neo4j.uri}") String uri,
            @Value("${neo4j.username}") String username,
            @Value("${neo4j.password}") String password
    ) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
    }
    public void executeWithoutResult(String query, Map<String, Object> params) {
        try (Session session = driver.session(SessionConfig.forDatabase(db))) {
            session.executeWriteWithoutResult(tx -> {
                tx.run(query, params);
            });
        }
    }
    public List<Record> fetch(String query) {
        try (Session session = driver.session(SessionConfig.forDatabase(db))) {;
            return session.run(query).list();
        }
    }

    @Override
    public void close() {
        if (driver != null) {
            driver.close();
        }
    }
}
