package ru.justnanix.wave.utils;

public class ThreadUtils {
    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Throwable ignored) {}
    }
}
