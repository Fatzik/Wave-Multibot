package ru.justnanix.wave.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import ru.justnanix.wave.Wave;
import ru.justnanix.wave.utils.Logger;
import ru.justnanix.wave.utils.Options;
import ru.justnanix.wave.utils.ThreadUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerParser {

    private List<String> servers = new CopyOnWriteArrayList<>();
    private int number = -1;

    private static final Pattern IP_PORT = Pattern.compile(
            "(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?):\\d{1,5}"
    );

    private static final Pattern DOMAIN_PORT = Pattern.compile(
            "([a-zA-Z0-9][a-zA-Z0-9._-]{1,60}\\.[a-zA-Z]{2,10})(?::(\\d{1,5}))?"
    );

    private static final List<String> PROXY_SOFTWARE = Arrays.asList(
            "bungeecord", "bungee", "velocity", "waterfall", "flamecord",
            "hexacord", "travertine", "lightfall", "gate",
            "nullcordx", "nullcord", "xenfork", "limbofilter", "limbo",
            "aegis", "nlogin", "protocolize", "geyser", "dragonproxy"
    );

    private static final String API_UA  = "Wave-1.5-Reworked-by-FATZE/ServerParser";
    private static final String API_URL = "https://api.mcsrvstat.us/3/";

    private static final String BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Safari/537.36";

    private static final Set<String> JUNK_DOMAINS = new HashSet<>(Arrays.asList(
            "google.com", "yandex.ru", "vk.com", "youtube.com", "github.com",
            "liveinternet.ru", "cloudflare.com", "minecraftrating.ru",
            "tmonitoring.com", "monitoringminecraft.ru", "monitoringminecraft.net",
            "misterlauncher.org", "minecraft-statistic.net", "mojang.com",
            "minecraft.net", "adoptium.net", "jitpack.io", "apache.org",
            "schema.org", "w3.org", "jquery.com", "bootstrapcdn.com"
    ));

    private static final Set<String> JUNK_SUFFIXES = new HashSet<>(Arrays.asList(
            ".png", ".jpg", ".jpeg", ".svg", ".css", ".js", ".ico",
            ".gif", ".woff", ".woff2", ".ttf", ".eot", ".webp", ".xml", ".json"
    ));

    // HttpClient с TLS настройками Java 11 — лучше проходит через WAF чем Jsoup
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    // ------------------------------------------------------------------ init ------------------------------------------------------------------

    public void init() {
        if (Options.testMode) return;

        Logger.parserStart("ServerParser");
        parseServers(true);
        Logger.parserDone("ServerParser", servers.size());
        Logger.separator();

        new Thread(() -> {
            while (true) {
                ThreadUtils.sleep(120000L);
                parseServers(false);
            }
        }, "server-reparse").start();
    }

    private void parseServers(boolean print) {
        Set<String> raw = new LinkedHashSet<>();

        raw.addAll(parseMinecraftRating(print));
        raw.addAll(parseTmonitoring(print));
        raw.addAll(parseMonitoringMinecraft(print));
        raw.addAll(parseMisterLauncher(print));
        raw.addAll(parseMinecraftStatistic(print));

        if (print)
            Logger.system("Сырых адресов: " + raw.size() + "  проверяю через mcsrvstat.us...");

        List<String> filtered = checkViaMcsrvstat(new ArrayList<>(raw), print);

        for (String s : filtered)
            if (!servers.contains(s)) servers.add(s);

        servers = new CopyOnWriteArrayList<>(new HashSet<>(servers));
        Collections.shuffle(servers, Wave.getInstance().getRandom());
    }

    // ------------------------------------------------------------------ парсеры ------------------------------------------------------------------

    /**
     * minecraftrating.ru
     * IP/домен находится в: <button class="item-control__copy"><span>IP_ИЛИ_ДОМЕН</span>
     */
    private List<String> parseMinecraftRating(boolean print) {
        List<String> result = new ArrayList<>();
        // Парсим первые 3 страницы новых серверов
        String[] urls = {
                "https://minecraftrating.ru/new-servers/",
                "https://minecraftrating.ru/new-servers/page/2/",
                "https://minecraftrating.ru/new-servers/page/3/"
        };
        for (String url : urls) {
            try {
                if (print) Logger.parserStart(url);
                String html = httpGet(url);
                Document doc = Jsoup.parse(html);

                for (Element btn : doc.select("button.item-control__copy")) {
                    Element span = btn.selectFirst("span");
                    if (span != null) result.addAll(extractFromShortText(span.text().trim()));
                }
                result.addAll(extractIpPort(html));

                if (print) Logger.system("minecraftrating.ru  " + result.size() + " адресов");
            } catch (Throwable e) {
                if (print) Logger.parserError("minecraftrating.ru", e.getMessage());
            }
        }
        return result;
    }

    /**
     * tmonitoring.com
     * IP:PORT прямо в тексте страницы.
     */
    private List<String> parseTmonitoring(boolean print) {
        List<String> result = new ArrayList<>();
        // Парсим первые 3 страницы новых серверов
        String[] urls = {
                "https://tmonitoring.com/servers-new/",
                "https://tmonitoring.com/servers-new/page/2/",
                "https://tmonitoring.com/servers-new/page/3/"
        };
        for (String url : urls) {
            try {
                if (print) Logger.parserStart(url);
                String html = httpGet(url);
                result.addAll(extractIpPort(html));
                Document doc = Jsoup.parse(html);
                for (Element a : doc.select("a[href*='/server/']")) {
                    String text = a.ownText().trim();
                    if (!text.isEmpty() && text.length() < 100)
                        result.addAll(extractFromShortText(text));
                }
                if (print) Logger.system("tmonitoring.com  " + result.size() + " адресов");
            } catch (Throwable e) {
                if (print) Logger.parserError("tmonitoring.com", e.getMessage());
            }
        }
        return result;
    }

    /**
     * monitoringminecraft.ru / .net
     */
    private List<String> parseMonitoringMinecraft(boolean print) {
        List<String> result = new ArrayList<>();
        String[] urls = {
                "https://monitoringminecraft.ru/",
                "https://monitoringminecraft.net/"
        };
        for (String url : urls) {
            try {
                if (print) Logger.parserStart(url);
                String html = httpGet(url);
                result.addAll(extractIpPort(html));
                if (print) Logger.system("monitoringminecraft  " + result.size() + " адресов");
            } catch (Throwable e) {
                if (print) Logger.parserError("monitoringminecraft", e.getMessage());
            }
        }
        return result;
    }

    /**
     * misterlauncher.org
     */
    private List<String> parseMisterLauncher(boolean print) {
        List<String> result = new ArrayList<>();
        try {
            if (print) Logger.parserStart("misterlauncher.org");
            String html = httpGet("https://misterlauncher.org/servera-novye/");
            result.addAll(extractIpPort(html));
            Document doc = Jsoup.parse(html);
            for (Element el : doc.select("[data-toggle='tooltip'], .tooltip"))
                result.addAll(extractFromShortText(el.ownText().trim()));
            if (print) Logger.system("misterlauncher.org  " + result.size() + " адресов");
        } catch (Throwable e) {
            if (print) Logger.parserError("misterlauncher.org", e.getMessage());
        }
        return result;
    }

    /**
     * minecraft-statistic.net
     */
    private List<String> parseMinecraftStatistic(boolean print) {
        List<String> result = new ArrayList<>();
        String[] urls = {
                "https://minecraft-statistic.net/ru/servers-new/",
                "https://minecraft-statistic.net/ru/servers/"
        };
        for (String url : urls) {
            try {
                if (print) Logger.parserStart(url);
                String html = httpGet(url);
                result.addAll(extractIpPort(html));
                Document doc = Jsoup.parse(html);
                for (Element el : doc.select(".copy-ip"))
                    result.addAll(extractFromShortText(el.text().trim()));
                if (print) Logger.system("minecraft-statistic.net  " + result.size() + " адресов");
            } catch (Throwable e) {
                if (print) Logger.parserError("minecraft-statistic.net", e.getMessage());
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ HTTP ------------------------------------------------------------------

    /**
     * Загружает страницу через java.net.http.HttpClient с браузерными заголовками.
     * Лучше проходит через WAF/Cloudflare чем Jsoup (разный TLS fingerprint).
     */
    private String httpGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("User-Agent",      BROWSER_UA)
                .header("Accept",          "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Upgrade-Insecure-Requests", "1")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400)
            throw new RuntimeException("HTTP " + response.statusCode());

        return response.body();
    }

    // ------------------------------------------------------------------ mcsrvstat API ------------------------------------------------------------------

    private List<String> checkViaMcsrvstat(List<String> addresses, boolean print) {
        List<String> result = new CopyOnWriteArrayList<>();
        List<Thread> threads = new ArrayList<>();

        for (String address : addresses) {
            Thread t = new Thread(() -> {
                try {
                    ServerInfo info = queryMcsrvstat(address);
                    if (info != null && info.online && !isProxySoftware(info.software)) {
                        String resolved = info.ip + ":" + info.port;
                        result.add(resolved);
                        if (print) Logger.serverFound(resolved, info.software);
                    } else if (print) {
                        if (info == null)
                            Logger.serverSkip(address, "нет ответа");
                        else if (!info.online)
                            Logger.serverSkip(address, "offline");
                        else
                            Logger.serverSkip(address, "proxy: " + info.software);
                    }
                } catch (Throwable ignored) {}
            });
            t.setDaemon(true);
            threads.add(t);
        }

        // Запускаем батчами по 15 — не перегружаем API
        int batchSize = 15;
        for (int i = 0; i < threads.size(); i += batchSize) {
            List<Thread> batch = threads.subList(i, Math.min(i + batchSize, threads.size()));
            batch.forEach(Thread::start);
            // Ждём завершения батча — join без прерывания
            for (Thread t : batch) {
                try { t.join(12000); } catch (InterruptedException ignored) {}
            }
        }

        return result;
    }

    private ServerInfo queryMcsrvstat(String address) {
        try {
            String json = Jsoup.connect(API_URL + address)
                    .userAgent(API_UA)
                    .header("Accept", "application/json")
                    .ignoreContentType(true)
                    .ignoreHttpErrors(false)
                    .timeout(10000)
                    .get()
                    .text();

            // Парсим JSON вручную — без Gson, чтобы избежать NoClassDefFoundError в потоках
            ServerInfo info = new ServerInfo();
            info.online = json.contains("\"online\":true");
            if (!info.online) return info;

            info.ip       = extractJsonString(json, "ip",       address.split(":")[0]);
            info.port     = extractJsonInt   (json, "port",     25565);
            info.software = extractJsonString(json, "software", null);
            return info;
        } catch (Throwable e) {
            Logger.parserError("mcsrvstat/" + address,
                    e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    /** Извлекает строковое поле из JSON: "key":"value" */
    private String extractJsonString(String json, String key, String def) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : def;
    }

    /** Извлекает числовое поле из JSON: "key":12345 */
    private int extractJsonInt(String json, String key, int def) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : def;
    }

    // ------------------------------------------------------------------ утилиты ------------------------------------------------------------------

    /** Извлекает IP:PORT из произвольного текста/HTML — быстро и безопасно. */
    private List<String> extractIpPort(String text) {
        List<String> result = new ArrayList<>();
        Matcher m = IP_PORT.matcher(text);
        while (m.find()) result.add(m.group());
        return result;
    }

    /**
     * Извлекает адрес из короткой строки (один элемент, не вся страница).
     * Сначала IP:PORT, потом домен.
     */
    private List<String> extractFromShortText(String text) {
        text = text.trim();
        if (text.isEmpty() || text.length() > 150) return Collections.emptyList();

        List<String> result = new ArrayList<>();

        Matcher m1 = IP_PORT.matcher(text);
        while (m1.find()) result.add(m1.group());
        if (!result.isEmpty()) return result;

        Matcher m2 = DOMAIN_PORT.matcher(text);
        while (m2.find()) {
            String domain = m2.group(1).toLowerCase();
            String port   = m2.group(2);
            if (isValidServerDomain(domain))
                result.add(port != null ? domain + ":" + port : domain);
        }

        return result;
    }

    private boolean isValidServerDomain(String domain) {
        if (domain.length() < 4 || !domain.contains(".")) return false;
        for (String s : JUNK_SUFFIXES)
            if (domain.endsWith(s)) return false;
        for (String junk : JUNK_DOMAINS)
            if (domain.equals(junk) || domain.endsWith("." + junk)) return false;
        return true;
    }

    private boolean isProxySoftware(String software) {
        if (software == null) return false;
        String lower = software.toLowerCase();
        for (String proxy : PROXY_SOFTWARE)
            if (lower.contains(proxy)) return true;
        return false;
    }

    private static class ServerInfo {
        boolean online;
        String  ip;
        int     port;
        String  software;
    }

    // ------------------------------------------------------------------ публичные методы ------------------------------------------------------------------

    public String nextServer() {
        ++number;
        if (number >= servers.size()) number = 0;
        return servers.get(number);
    }

    public List<String> getServers() {
        return servers;
    }
}
