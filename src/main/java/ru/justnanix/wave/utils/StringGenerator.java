package ru.justnanix.wave.utils;

import ru.justnanix.wave.Wave;

public class StringGenerator {

    private static final char[] DIGITS  = "0123456789".toCharArray();
    private static final char[] LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    /** Только цифры — используется для паролей */
    public static String generateStringInt(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(DIGITS[Wave.getInstance().getRandom().nextInt(DIGITS.length)]);
        return sb.toString();
    }

    /** Буквы + цифры — используется для генерации ников */
    public static String generateNick(int length) {
        StringBuilder sb = new StringBuilder(length);
        // Первый символ — всегда буква (Minecraft не принимает ники начинающиеся с цифры)
        sb.append((char)('a' + Wave.getInstance().getRandom().nextInt(26)));
        for (int i = 1; i < length; i++)
            sb.append(LETTERS[Wave.getInstance().getRandom().nextInt(LETTERS.length)]);
        return sb.toString();
    }
}
