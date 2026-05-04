package ru.justnanix.wave.utils;

import ru.justnanix.wave.Wave;

public class StringGenerator {
    public static String generateStringInt(int length) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < length; i++)
            builder.append(Wave.getInstance().getRandom().nextInt(10));

        return builder.toString();
    }
}
