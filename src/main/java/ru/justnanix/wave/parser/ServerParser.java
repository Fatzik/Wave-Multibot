package ru.justnanix.wave.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import ru.justnanix.wave.Wave;
import ru.justnanix.wave.utils.Logger;
import ru.justnanix.wave.utils.Options;
import ru.justnanix.wave.utils.ThreadUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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
            "schema.org", "w3.org", "jquery.com", "bootstrapcdn.com",
            "gamemonitoring.ru", "minecraft.menu", "topminecraftservers.org",
            "minecraft-server-list.com", "mcsrvstat.us", "api.mcsrvstat.us"
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

        // Запускаем парсинг полностью в фоне — боты стартуют как только появится первый сервер
        new Thread(() -> {
            parseServers(true);
            Logger.parserDone("ServerParser", servers.size());
            Logger.separator();

            // Перепарсинг каждые 2 минуты
            while (true) {
                ThreadUtils.sleep(120000L);
                parseServers(false);
            }
        }, "server-parser").start();
    }

    /** Ждёт пока не появится хотя бы один сервер (вызывается из Wave перед стартом ботов) */
    public void waitForFirst() {
        if (Options.testMode) return;
        while (servers.isEmpty()) {
            ThreadUtils.sleep(200L);
        }
    }

    private void parseServers(boolean print) {
        Set<String> raw = new LinkedHashSet<>();

        raw.addAll(parseMinecraftRating(print));
        raw.addAll(parseTmonitoring(print));
        raw.addAll(parseGamemonitoring(print));
        raw.addAll(parseMinecraftMenu(print));
        raw.addAll(parseTopMinecraftServers(print));
        raw.addAll(parseMinecraftServerList(print));

        // Убираем уже известные серверы чтобы не проверять повторно
        Set<String> known = new HashSet<>(servers);
        List<String> toCheck = new ArrayList<>();
        for (String s : raw) {
            if (!known.contains(s)) toCheck.add(s);
        }

        if (print)
            Logger.system("Сырых адресов: " + toCheck.size() + "  проверяю через mcstatus.io...");

        // Проверяем — серверы добавляются в список сразу по мере нахождения
        checkServers(toCheck, print);

        // Дедупликация и перемешивание
        servers = new CopyOnWriteArrayList<>(new HashSet<>(servers));
        Collections.shuffle(servers, Wave.getInstance().getRandom());
    }

    // ------------------------------------------------------------------ парсеры ------------------------------------------------------------------

    /** minecraftrating.ru — IP/домен в <button class="item-control__copy"><span> */
    private List<String> parseMinecraftRating(boolean print) {
        List<String> result = new ArrayList<>();
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

    /** tmonitoring.com — IP:PORT в тексте страницы */
    private List<String> parseTmonitoring(boolean print) {
        List<String> result = new ArrayList<>();
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

    /** gamemonitoring.ru — IP:PORT и домены прямо в тексте таблицы */
    private List<String> parseGamemonitoring(boolean print) {
        List<String> result = new ArrayList<>();
        String[] urls = {
                "https://gamemonitoring.ru/minecraft/servers/new/version/minecraft",
                "https://gamemonitoring.ru/minecraft/servers/country/ru",
                "https://gamemonitoring.ru/minecraft/servers/country/ua",
                "https://gamemonitoring.ru/minecraft/servers/country/by"
        };
        for (String url : urls) {
            try {
                if (print) Logger.parserStart(url);
                String html = httpGet(url);
                // IP:PORT regex
                result.addAll(extractIpPort(html));
                // Домены из ячеек таблицы
                Document doc = Jsoup.parse(html);
                for (Element td : doc.select("td")) {
                    String text = td.ownText().trim();
                    if (text.contains(".") && text.length() < 80)
                        result.addAll(extractFromShortText(text));
                }
                if (print) Logger.system("gamemonitoring.ru  " + result.size() + " адресов");
            } catch (Throwable e) {
                if (print) Logger.parserError("gamemonitoring.ru", e.getMessage());
            }
        }
        return result;
    }

    /** minecraft.menu — домены серверов в тексте карточек */
    private List<String> parseMinecraftMenu(boolean print) {
        List<String> result = new ArrayList<>();
        String[] urls = {
                "https://minecraft.menu/minecraft-russia-servers",
                "https://minecraft.menu/minecraft-servers/new",
                "https://minecraft.menu/minecraft-servers"
        };
        for (String url : urls) {
            try {
                if (print) Logger.parserStart(url);
                String html = httpGet(url);
                result.addAll(extractIpPort(html));
                Document doc = Jsoup.parse(html);
                // Домены в параграфах и span
                for (Element el : doc.select("p, span, td, .server-ip, .ip")) {
                    String text = el.ownText().trim();
                    if (text.contains(".") && text.length() < 80 && !text.contains(" "))
                        result.addAll(extractFromShortText(text));
                }
                if (print) Logger.system("minecraft.menu  " + result.size() + " адресов");
            } catch (Throwable e) {
                if (print) Logger.parserError("minecraft.menu", e.getMessage());
            }
        }
        return result;
    }

    /** topminecraftservers.org — IP в data-атрибутах и тексте */
    private List<String> parseTopMinecraftServers(boolean print) {
        List<String> result = new ArrayList<>();
        String[] urls = {
                "https://topminecraftservers.org/",
                "https://topminecraftservers.org/country/RU",
                "https://topminecraftservers.org/new"
        };
        for (String url : urls) {
            try {
                if (print) Logger.parserStart(url);
                String html = httpGet(url);
                result.addAll(extractIpPort(html));
                Document doc = Jsoup.parse(html);
                for (Element el : doc.select("[data-clipboard-text], .server-ip, .ip-address, .copy")) {
                    String text = el.attr("data-clipboard-text");
                    if (text.isEmpty()) text = el.ownText();
                    text = text.trim();
                    if (!text.isEmpty() && text.length() < 80)
                        result.addAll(extractFromShortText(text));
                }
                if (print) Logger.system("topminecraftservers.org  " + result.size() + " адресов");
            } catch (Throwable e) {
                if (print) Logger.parserError("topminecraftservers.org", e.getMessage());
            }
        }
        return result;
    }

    /** minecraft-server-list.com — IP в data-clipboard и тексте */
    private List<String> parseMinecraftServerList(boolean print) {
        List<String> result = new ArrayList<>();
        String[] urls = {
                "https://minecraft-server-list.com/",
                "https://minecraft-server-list.com/sort/New/"
        };
        for (String url : urls) {
            try {
                if (print) Logger.parserStart(url);
                String html = httpGet(url);
                result.addAll(extractIpPort(html));
                Document doc = Jsoup.parse(html);
                for (Element el : doc.select("[data-clipboard-text], .server-ip, .serverip")) {
                    String text = el.attr("data-clipboard-text");
                    if (text.isEmpty()) text = el.ownText();
                    text = text.trim();
                    if (!text.isEmpty() && text.length() < 80)
                        result.addAll(extractFromShortText(text));
                }
                if (print) Logger.system("minecraft-server-list.com  " + result.size() + " адресов");
            } catch (Throwable e) {
                if (print) Logger.parserError("minecraft-server-list.com", e.getMessage());
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

    // ------------------------------------------------------------------ mcstatus.io API ------------------------------------------------------------------

    /**
     * Проверяет серверы через api.mcstatus.io/v2 — надёжный API без ложных offline.
     * Лимит: 5 req/sec. Слём последовательно с задержкой 220мс между запросами.
     */
    private void checkServers(List<String> addresses, boolean print) {
        for (String address : addresses) {
            try {
                ServerInfo info = queryMcstatus(address);
                if (info != null && info.online && !isProxySoftware(info.software)) {
                    // Добавляем сразу — боты могут использовать уже сейчас
                    if (!servers.contains(address)) servers.add(address);
                    if (print) Logger.serverFound(address, info.software);
                } else if (print) {
                    if (info == null)
                        Logger.serverSkip(address, "нет ответа");
                    else if (!info.online)
                        Logger.serverSkip(address, "offline");
                    else
                        Logger.serverSkip(address, "proxy: " + info.software);
                }
            } catch (Throwable ignored) {}

            // 220мс = ~4.5 req/sec — чуть ниже лимита 5/sec
            try { Thread.sleep(220); } catch (InterruptedException ignored) {}
        }
    }

    private ServerInfo queryMcstatus(String address) {
        try {
            String url = "https://api.mcstatus.io/v2/status/java/" + address;
            String json = Jsoup.connect(url)
                    .userAgent(API_UA)
                    .header("Accept", "application/json")
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .timeout(8000)
                    .get()
                    .text();

            ServerInfo info = new ServerInfo();
            info.online = json.contains("\"online\":true");
            if (!info.online) return info;

            // software из query (точнее) или version.name
            String software = extractJsonString(json, "software", null);
            if (software == null) {
                // version.name — берём первое слово (например "Paper 1.20.1" → "Paper")
                String vname = extractJsonString(json, "name", null);
                if (vname != null) {
                    vname = vname.replaceAll("§.", "").trim();
                    software = vname.split("\\s+")[0];
                }
            }
            info.software = software;
            return info;
        } catch (Throwable e) {
            return null;
        }
    }

    /** Извлекает строковое поле из JSON: "key":"value" */
    private String extractJsonString(String json, String key, String def) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : def;
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
