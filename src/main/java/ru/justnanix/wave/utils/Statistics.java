package ru.justnanix.wave.utils;

import ru.justnanix.wave.bot.Bot;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Statistics {
    public static final List<Bot> botList = new CopyOnWriteArrayList<>();

    public static final AtomicInteger solvedCaptcha = new AtomicInteger();
    public static final AtomicInteger messagesSent = new AtomicInteger();
}
