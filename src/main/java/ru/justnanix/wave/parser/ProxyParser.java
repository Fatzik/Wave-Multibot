package ru.justnanix.wave.parser;

import org.jsoup.Jsoup;
import ru.justnanix.wave.Wave;
import ru.justnanix.wave.utils.Logger;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ProxyParser {

    /**
     * Обёртка над java.net.Proxy с явным указанием типа (SOCKS4 / SOCKS5 / HTTP).
     * java.net.Proxy не различает SOCKS4 и SOCKS5 — храним тип отдельно.
     */
    public static class ProxyEntry {
        public enum Type { SOCKS4, SOCKS5, HTTP }

        public final Proxy  proxy;
        public final Type   type;
        public final String host;
        public final int    port;

        public ProxyEntry(Type type, String host, int port) {
            this.type = type;
            this.host = host;
            this.port = port;
            Proxy.Type javaType = (type == Type.HTTP) ? Proxy.Type.HTTP : Proxy.Type.SOCKS;
            this.proxy = new Proxy(javaType, new InetSocketAddress(host, port));
        }

        @Override
        public String toString() { return type.name().toLowerCase() + "://" + host + ":" + port; }
    }

    private List<ProxyEntry> proxies = new CopyOnWriteArrayList<>();
    private int number = -1;

    // ── Локальные файлы ──────────────────────────────────────────────────────
    private static final File FILE_SOCKS4 = new File("Proxy/socks4.txt");
    private static final File FILE_SOCKS5 = new File("Proxy/socks5.txt");
    private static final File FILE_HTTP   = new File("Proxy/http.txt");

    // ── GitHub источники (обновляются ежедневно/ежечасно) ───────────────────
    private static final String[] SOURCES_SOCKS4 = {
        // TheSpeedX — обновляется каждый день, ~1600 прокси
        "https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/socks4.txt",
        // ShiftyTR — обновляется часто
        "https://raw.githubusercontent.com/ShiftyTR/Proxy-List/master/socks4.txt",
        // monosans — обновляется каждый час
        "https://raw.githubusercontent.com/monosans/proxy-list/main/proxies/socks4.txt",
        // mmpx12
        "https://raw.githubusercontent.com/mmpx12/proxy-list/master/socks4.txt",
        // vakhov/fresh-proxy-list
        "https://raw.githubusercontent.com/vakhov/fresh-proxy-list/master/socks4.txt",
        // proxifly
        "https://raw.githubusercontent.com/proxifly/free-proxy-list/main/proxies/protocols/socks4/data.txt",
    };

    private static final String[] SOURCES_SOCKS5 = {
        // TheSpeedX — ~2400 прокси
        "https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/socks5.txt",
        // mmpx12 (твоя ссылка)
        "https://raw.githubusercontent.com/mmpx12/proxy-list/master/socks5.txt",
        // ShiftyTR
        "https://raw.githubusercontent.com/ShiftyTR/Proxy-List/master/socks5.txt",
        // monosans — каждый час
        "https://raw.githubusercontent.com/monosans/proxy-list/main/proxies/socks5.txt",
        // vakhov
        "https://raw.githubusercontent.com/vakhov/fresh-proxy-list/master/socks5.txt",
        // proxifly
        "https://raw.githubusercontent.com/proxifly/free-proxy-list/main/proxies/protocols/socks5/data.txt",
        // hookzof
        "https://raw.githubusercontent.com/hookzof/socks5_list/master/proxy.txt",
        // B4RC0D3D
        "https://raw.githubusercontent.com/B4RC0D3D/proxy-list/main/SOCKS5.txt",
    };

    private static final String[] SOURCES_HTTP = {
        // TheSpeedX — ~2000 прокси
        "https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/http.txt",
        // ShiftyTR
        "https://raw.githubusercontent.com/ShiftyTR/Proxy-List/master/http.txt",
        // monosans — каждый час
        "https://raw.githubusercontent.com/monosans/proxy-list/main/proxies/http.txt",
        // mmpx12
        "https://raw.githubusercontent.com/mmpx12/proxy-list/master/http.txt",
        // vakhov
        "https://raw.githubusercontent.com/vakhov/fresh-proxy-list/master/http.txt",
        // proxifly
        "https://raw.githubusercontent.com/proxifly/free-proxy-list/main/proxies/protocols/http/data.txt",
        // clarketm
        "https://raw.githubusercontent.com/clarketm/proxy-list/master/proxy-list-raw.txt",
        // sunny9577
        "https://raw.githubusercontent.com/sunny9577/proxy-scraper/master/proxies.txt",
    };

    // ── Init ─────────────────────────────────────────────────────────────────

    public void init() {
        Logger.parserStart("ProxyParser");
        new File("Proxy").mkdirs();

        // Сначала пробуем локальные файлы
        int loaded = 0;
        loaded += loadFromFile(FILE_SOCKS4, ProxyEntry.Type.SOCKS4);
        loaded += loadFromFile(FILE_SOCKS5, ProxyEntry.Type.SOCKS5);
        loaded += loadFromFile(FILE_HTTP,   ProxyEntry.Type.HTTP);

        if (loaded > 0) {
            shuffle();
            Logger.parserDone("ProxyParser", proxies.size());
            logSummary();
            return;
        }

        // Локальных нет — качаем из всех источников параллельно
        Logger.system("ProxyParser  локальные файлы не найдены, качаю из сети...");
        downloadAll();

        shuffle();
        Logger.parserDone("ProxyParser", proxies.size());
        logSummary();

        // Сохраняем по файлам для следующего запуска
        saveToFile(FILE_SOCKS4, ProxyEntry.Type.SOCKS4);
        saveToFile(FILE_SOCKS5, ProxyEntry.Type.SOCKS5);
        saveToFile(FILE_HTTP,   ProxyEntry.Type.HTTP);
    }

    // ── Скачивание всех источников параллельно ───────────────────────────────

    private void downloadAll() {
        List<Thread> threads = new ArrayList<>();

        for (String url : SOURCES_SOCKS4)
            threads.add(new Thread(() -> addAll(downloadRaw(url), ProxyEntry.Type.SOCKS4)));
        for (String url : SOURCES_SOCKS5)
            threads.add(new Thread(() -> addAll(downloadRaw(url), ProxyEntry.Type.SOCKS5)));
        for (String url : SOURCES_HTTP)
            threads.add(new Thread(() -> addAll(downloadRaw(url), ProxyEntry.Type.HTTP)));

        threads.forEach(t -> { t.setDaemon(true); t.start(); });
        for (Thread t : threads) {
            try { t.join(12000); } catch (InterruptedException ignored) {}
        }

        // Дедупликация
        Set<String> seen = new HashSet<>();
        List<ProxyEntry> deduped = new ArrayList<>();
        for (ProxyEntry e : proxies)
            if (seen.add(e.toString())) deduped.add(e);
        proxies = new CopyOnWriteArrayList<>(deduped);
    }

    private void addAll(List<String> lines, ProxyEntry.Type type) {
        for (String line : lines) {
            ProxyEntry e = parse(line, type);
            if (e != null) proxies.add(e);
        }
    }

    // ── Скачивание одного источника ──────────────────────────────────────────

    private List<String> downloadRaw(String url) {
        try {
            String text = Jsoup.connect(url)
                    .userAgent("Wave-1.5/ProxyParser")
                    .ignoreContentType(true)
                    .timeout(10000)
                    .get()
                    .text();
            return Arrays.asList(text.split("\\s+"));
        } catch (Throwable e) {
            Logger.parserError("ProxyParser", url.replaceAll("https://raw.githubusercontent.com/", "") + " — " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Загрузка из локального файла ─────────────────────────────────────────

    private int loadFromFile(File file, ProxyEntry.Type type) {
        if (!file.exists()) return 0;
        int count = 0;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                ProxyEntry e = parse(line, type);
                if (e != null) { proxies.add(e); count++; }
            }
        } catch (Exception e) {
            Logger.parserError("ProxyParser/" + file.getName(), e.getMessage());
        }
        if (count > 0)
            Logger.system("ProxyParser  " + file.getName() + "  загружено " + count);
        return count;
    }

    // ── Сохранение в файл ────────────────────────────────────────────────────

    private void saveToFile(File file, ProxyEntry.Type type) {
        List<ProxyEntry> list = proxies.stream()
                .filter(e -> e.type == type)
                .collect(Collectors.toList());
        if (list.isEmpty()) return;
        try {
            file.createNewFile();
            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                for (ProxyEntry e : list) w.write(e.host + ":" + e.port + "\n");
            }
        } catch (Exception ignored) {}
    }

    // ── Парсинг строки ip:port ────────────────────────────────────────────────

    private ProxyEntry parse(String line, ProxyEntry.Type type) {
        try {
            if (line == null) return null;
            line = line.trim();
            // Убираем префикс типа если есть (socks5://1.2.3.4:1080)
            if (line.contains("://")) line = line.split("://", 2)[1];
            // Убираем всё после пробела (некоторые списки добавляют страну)
            if (line.contains(" ")) line = line.split(" ")[0];
            String[] parts = line.split(":");
            if (parts.length < 2) return null;
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            if (host.isEmpty() || port < 1 || port > 65535) return null;
            return new ProxyEntry(type, host, port);
        } catch (Exception e) {
            return null;
        }
    }

    // ── Утилиты ──────────────────────────────────────────────────────────────

    private void shuffle() {
        Collections.shuffle(proxies, Wave.getInstance().getRandom());
    }

    private void logSummary() {
        long s4 = proxies.stream().filter(e -> e.type == ProxyEntry.Type.SOCKS4).count();
        long s5 = proxies.stream().filter(e -> e.type == ProxyEntry.Type.SOCKS5).count();
        long ht = proxies.stream().filter(e -> e.type == ProxyEntry.Type.HTTP).count();
        Logger.system("ProxyParser  SOCKS4: " + s4 + "  SOCKS5: " + s5 + "  HTTP: " + ht);
    }

    // ── Публичные методы ─────────────────────────────────────────────────────

    public ProxyEntry nextProxy() {
        if (proxies.isEmpty()) return null;
        ++number;
        if (number >= proxies.size()) number = 0;
        return proxies.get(number);
    }

    public List<ProxyEntry> getProxies() {
        return proxies;
    }
}
