package ru.justnanix.wave.utils;

import java.util.ArrayList;

public class Options {
    public static int infoFormat;

    public static int botsCount;
    public static int joinDelay;

    public static boolean randomNicks;
    public static int randomNicksLength;

    public static boolean randomPasswords;
    public static int randomPasswordsLength;

    public static boolean doubleJoin;
    public static boolean antiBotFilter;

    public static boolean testMode;
    public static String testModeIp;

    public static boolean autoRestart;
    public static int autoRestartDelay;

    public static boolean move;

    public static ArrayList commands;
    public static int commandDelay;      // задержка между командами (мс)
    public static int commandLoopDelay;  // задержка между циклами команд (мс)
}
