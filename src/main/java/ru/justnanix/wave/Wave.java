package ru.justnanix.wave;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import org.yaml.snakeyaml.Yaml;
import ru.justnanix.wave.bot.Bot;
import ru.justnanix.wave.parser.NicksParser;
import ru.justnanix.wave.parser.ProxyParser;
import ru.justnanix.wave.parser.ServerParser;
import ru.justnanix.wave.utils.Options;
import ru.justnanix.wave.utils.Statistics;
import ru.justnanix.wave.utils.StringGenerator;
import ru.justnanix.wave.utils.ThreadUtils;

import ru.justnanix.wave.utils.Logger;

import java.io.FileInputStream;
import java.net.Proxy;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Wave {
    private static Wave instance;

    private final Random random = new Random(System.currentTimeMillis());

    private final ServerParser serverParser = new ServerParser();
    private final ProxyParser proxyParser = new ProxyParser();
    private final NicksParser nicksParser = new NicksParser();

    {
        Logger.enableAnsi();
        Logger.banner();
        System.setProperty("socksProxyVersion", "4");

        if (System.console() == null) {
            try {
                new ProcessBuilder("cmd", "/c", "msg", System.getenv("username"),
                        "запусти через Старт.bat!").start();
            } catch (Exception ignored) {}
            System.exit(0);
        }

        try {
            new ProcessBuilder("cmd", "/c", "title Wave 1.5 Reworked by FATZE")
                    .inheritIO().start().waitFor();
        } catch (Exception ignored) {}

        try {
            Map<String, Object> values = new Yaml().load(new FileInputStream("config.yml"));

            {
                Options.infoFormat = (int) values.get("infoFormat");

                Options.botsCount = (int) values.get("botsCount");
                Options.joinDelay = (int) values.get("joinDelay");

                Options.randomNicks = (boolean) values.get("randomNicks");
                Options.randomNicksLength = (int) values.get("randomNicksLength");

                Options.randomPasswords = (boolean) values.get("randomPasswords");
                Options.randomPasswordsLength = (int) values.get("randomPasswordsLength");

                Options.doubleJoin = (boolean) values.get("doubleJoin");
                Options.antiBotFilter = (boolean) values.get("antiBotFilter");

                Options.testMode = (boolean) values.get("testMode");
                Options.testModeIp = (String) values.get("testModeIp");

                Options.autoRestart = (boolean) values.get("autoRestart");
                Options.autoRestartDelay = (int) values.get("autoRestartDelay");

                Options.move = (boolean) values.get("move");

                Options.commands = (ArrayList) values.get("commands");

                Options.commandDelay     = values.containsKey("commandDelay")     ? (int) values.get("commandDelay")     : 1000;
                Options.commandLoopDelay = values.containsKey("commandLoopDelay") ? (int) values.get("commandLoopDelay") : 5000;
            }

            instance = this;
        } catch (Exception e) {
            Logger.error("Ошибка загрузки config.yml: " + e.getMessage());
            ThreadUtils.sleep(2000L);
            e.printStackTrace();
            ThreadUtils.sleep(10000L);
            System.exit(0);
        }

        System.err.close();
    }

    public void launch() {
        Logger.system("Запуск Wave...");
        Logger.separator();

        proxyParser.init();
        serverParser.init();
        nicksParser.init();

        if (Options.autoRestart) {
            new Thread(() -> {
                try {
                    ThreadUtils.sleep(TimeUnit.MINUTES.toMillis(Options.autoRestartDelay));

                    String path = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                    String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8).substring(1).replace("/", "\\");

                    new ProcessBuilder("cmd", "/c", "start", "java", "-Xmx2G", "-server", "-jar", "\"" + decodedPath + "\"").start();

                    System.exit(0);
                } catch (Exception ignored) {}
            }).start();
        }

        if (Options.infoFormat > 0) {
            new Thread(() -> {
                while (true) {
                    ThreadUtils.sleep(2500L);
                    Logger.stats(
                        Statistics.botList.size(),
                        Statistics.solvedCaptcha.get(),
                        Statistics.messagesSent.get()
                    );
                }
            }, "stats-thread").start();
        }

        while (true) {
            Proxy proxy = proxyParser.nextProxy();

            String server = Options.testMode ? Options.testModeIp : serverParser.nextServer();
            String nick = Options.randomNicks ? StringGenerator.generateStringInt(Options.randomNicksLength) : nicksParser.nextNick();

            for (int i = 0; i < Options.botsCount; i++) {
                try {
                    String host = server.split(":")[0];
                    int port = Integer.parseInt(server.split(":")[1]);

                    new Thread(
                            () -> new Bot(new MinecraftProtocol(nick), host, port, proxy).connect()
                    ).start();
                } catch (OutOfMemoryError ignored) {}
            }

            ThreadUtils.sleep(Options.joinDelay);
        }
    }

    public static Wave getInstance() {
        return instance;
    }

    public ServerParser getServerParser() {
        return serverParser;
    }

    public ProxyParser getProxyParser() {
        return proxyParser;
    }

    public NicksParser getNicksParser() {
        return nicksParser;
    }

    public Random getRandom() {
        return random;
    }
}
