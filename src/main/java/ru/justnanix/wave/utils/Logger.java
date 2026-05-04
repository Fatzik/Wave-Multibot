package ru.justnanix.wave.utils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Цветной вывод в консоль через ANSI escape-коды.
 * Windows 10+ (1511+) поддерживает ANSI в cmd/PowerShell нативно.
 */
public class Logger {

    // ── ANSI цвета ──────────────────────────────────────────────────────────
    private static final String RESET   = "\u001B[0m";
    private static final String BOLD    = "\u001B[1m";
    private static final String DIM     = "\u001B[2m";

    private static final String BLACK   = "\u001B[30m";
    private static final String RED     = "\u001B[31m";
    private static final String GREEN   = "\u001B[32m";
    private static final String YELLOW  = "\u001B[33m";
    private static final String BLUE    = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String CYAN    = "\u001B[36m";
    private static final String WHITE   = "\u001B[37m";

    private static final String B_RED     = "\u001B[91m";
    private static final String B_GREEN   = "\u001B[92m";
    private static final String B_YELLOW  = "\u001B[93m";
    private static final String B_BLUE    = "\u001B[94m";
    private static final String B_MAGENTA = "\u001B[95m";
    private static final String B_CYAN    = "\u001B[96m";
    private static final String B_WHITE   = "\u001B[97m";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ── Включить ANSI на Windows ─────────────────────────────────────────────
    public static void enableAnsi() {
        try {
            // Включаем VIRTUAL_TERMINAL_PROCESSING через kernel32
            new ProcessBuilder("cmd", "/c", "").start();
        } catch (Exception ignored) {}
        // Устанавливаем кодировку UTF-8
        System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8));
    }

    // ── Внутренний форматтер ─────────────────────────────────────────────────
    private static String time() {
        return DIM + "[" + LocalTime.now().format(TIME_FMT) + "]" + RESET;
    }

    private static String tag(String label, String color) {
        return color + BOLD + "[" + label + "]" + RESET;
    }

    // ── Публичные методы ─────────────────────────────────────────────────────

    /** Баннер при запуске */
    public static void banner() {
        System.out.println();
        System.out.println(B_CYAN + BOLD +
                "  ██╗    ██╗ █████╗ ██╗   ██╗███████╗" + RESET);
        System.out.println(B_CYAN + BOLD +
                "  ██║    ██║██╔══██╗██║   ██║██╔════╝" + RESET);
        System.out.println(B_CYAN + BOLD +
                "  ██║ █╗ ██║███████║██║   ██║█████╗  " + RESET);
        System.out.println(B_CYAN + BOLD +
                "  ██║███╗██║██╔══██║╚██╗ ██╔╝██╔══╝  " + RESET);
        System.out.println(B_CYAN + BOLD +
                "  ╚███╔███╔╝██║  ██║ ╚████╔╝ ███████╗" + RESET);
        System.out.println(B_CYAN + BOLD +
                "   ╚══╝╚══╝ ╚═╝  ╚═╝  ╚═══╝  ╚══════╝" + RESET);
        System.out.println();
        System.out.println("  " + B_WHITE + BOLD + "v1.5 Reworked" + RESET +
                "  " + DIM + "by " + RESET + B_MAGENTA + BOLD + "FATZE" + RESET);
        System.out.println("  " + DIM + "Создано при поддержке " + RESET +
                CYAN + "https://t.me/nnsquado" + RESET);
        System.out.println();
        System.out.println("  " + DIM + "─────────────────────────────────────" + RESET);
        System.out.println();
    }

    /** Системное сообщение (серый) */
    public static void system(String msg) {
        System.out.println(time() + " " + tag("SYS", B_BLUE) + "  " + WHITE + msg + RESET);
    }

    /** Успех / подключение (зелёный) */
    public static void success(String msg) {
        System.out.println(time() + " " + tag(" OK", B_GREEN) + "  " + B_GREEN + msg + RESET);
    }

    /** Предупреждение (жёлтый) */
    public static void warn(String msg) {
        System.out.println(time() + " " + tag("WRN", B_YELLOW) + "  " + B_YELLOW + msg + RESET);
    }

    /** Ошибка / дисконнект (красный) */
    public static void error(String msg) {
        System.out.println(time() + " " + tag("ERR", B_RED) + "  " + B_RED + msg + RESET);
    }

    /** Информация о боте (голубой) */
    public static void bot(String server, String nick, String msg) {
        System.out.println(time() + " " + tag("BOT", B_CYAN) +
                "  " + DIM + server + RESET +
                "  " + CYAN + nick + RESET +
                "  " + WHITE + msg + RESET);
    }

    /** Чат-сообщение от сервера */
    public static void chat(String server, String nick, String msg) {
        System.out.println(time() + " " + tag("CHT", MAGENTA) +
                "  " + DIM + server + RESET +
                "  " + CYAN + nick + RESET +
                "  " + WHITE + msg + RESET);
    }

    /** Статистика (периодический вывод) */
    public static void stats(int bots, int captcha, int messages) {
        System.out.println(
                time() + " " + tag("INF", B_MAGENTA) +
                "  Ботов: " + B_GREEN + BOLD + bots + RESET +
                "  Капч: " + B_YELLOW + BOLD + captcha + RESET +
                "  Сообщений: " + B_CYAN + BOLD + messages + RESET
        );
    }

    /** Парсер — начало */
    public static void parserStart(String name) {
        System.out.println(time() + " " + tag("PSR", B_BLUE) + "  " + B_BLUE + name + RESET + "  " + DIM + "запускаю..." + RESET);
    }

    /** Парсер — результат */
    public static void parserDone(String name, int count) {
        System.out.println(time() + " " + tag("PSR", B_BLUE) + "  " + B_BLUE + name + RESET +
                "  загружено " + B_WHITE + BOLD + count + RESET);
    }

    /** Парсер — ошибка */
    public static void parserError(String name, String err) {
        System.out.println(time() + " " + tag("PSR", B_RED) + "  " + B_RED + name + RESET +
                "  " + DIM + err + RESET);
    }

    /** Сервер найден через API */
    public static void serverFound(String addr, String software) {
        System.out.println(time() + " " + tag(" + ", B_GREEN) +
                "  " + B_GREEN + BOLD + String.format("%-30s", addr) + RESET +
                "  " + DIM + (software != null ? software : "unknown") + RESET);
    }

    /** Сервер отклонён */
    public static void serverSkip(String addr, String reason) {
        System.out.println(time() + " " + tag(" - ", DIM) +
                "  " + DIM + String.format("%-30s", addr) + "  " + reason + RESET);
    }

    /** AntiBotFilter */
    public static void antiBotFilter(String server) {
        System.out.println(time() + " " + tag("ABF", B_RED) +
                "  " + B_RED + "Удаляю сервер " + BOLD + server + RESET);
    }

    /** Разделитель */
    public static void separator() {
        System.out.println("  " + DIM + "─────────────────────────────────────" + RESET);
    }
}
