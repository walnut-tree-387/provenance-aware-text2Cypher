package com.example.text2cypher.utils;

public class SleeperCoach {
    public static void sleepMinutes(long milSec) {
        try {
            Thread.sleep(milSec);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread was interrupted while sleeping", e);
        }
    }
}

