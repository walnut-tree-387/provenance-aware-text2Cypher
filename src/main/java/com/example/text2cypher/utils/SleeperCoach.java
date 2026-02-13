package com.example.text2cypher.utils;

public class SleeperCoach {
    public static void sleepMinutes(long minutes) {
        try {
            Thread.sleep(minutes * 60_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread was interrupted while sleeping", e);
        }
    }
}

