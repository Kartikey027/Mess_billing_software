package com.smvdu.mess.service;

import java.util.HashMap;
import java.util.Map;

public class MessStatsCache {

    private static final Map<String, MessStats> CACHE = new HashMap<>();

    private static String key(int messId, int month, int year) {
        return messId + "-" + month + "-" + year;
    }

    public static MessStats get(int messId, int month, int year) {
        return CACHE.get(key(messId, month, year));
    }

    public static void put(int messId, int month, int year, MessStats stats) {
        CACHE.put(key(messId, month, year), stats);
    }

    public static void invalidate(int messId, int month, int year) {
        CACHE.remove(key(messId, month, year));
    }

    public static void clearAll() {
        CACHE.clear();
    }
}
