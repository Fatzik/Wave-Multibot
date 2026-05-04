package ru.justnanix.wave.utils;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import org.apache.commons.lang3.StringUtils;
import ru.justnanix.wave.bot.Bot;

import java.util.regex.Pattern;

public class CaptchaUtils {
    public static void solveCaptcha(String message, Bot client) {
        // .say /verify *** | Captcha

        try {
            String line = FindUtils.findStringByRegex(message, Pattern.compile("\\..* /verify .*")).replace("\"", " ");

            if (StringUtils.containsIgnoreCase(message, "clickEvent")) {
                client.getSession().send(new ClientChatPacket(line.split(" ")[0] + " /verify " + line.split(" ")[2]));
                client.register();

                Statistics.solvedCaptcha.getAndIncrement();
            }
        } catch (Exception ignored) {}

        // Authme | Captcha

        try {
            String line = FindUtils.findStringByRegex(message, Pattern.compile("/captcha .*")).replace("»", " ");

            if (line.contains(":"))
                client.getSession().send(new ClientChatPacket("/captcha " + line.split(":")[1].trim()));

            client.getSession().send(new ClientChatPacket("/captcha " + line.split(" ")[1]));
            client.register();

            Statistics.solvedCaptcha.getAndIncrement();
        } catch (Exception ignored) {}

        // Type **** | Captcha

        try {
            String line = FindUtils.findStringByRegex(message, Pattern.compile("Type .*"));

            if (StringUtils.containsIgnoreCase(message, "prove")) {
                client.getSession().send(new ClientChatPacket(line.split(" ")[1]));
                client.register();

                Statistics.solvedCaptcha.getAndIncrement();
            }
        } catch (Exception ignored) {}

        // Enter the following code | Captcha

        try {
            String line = FindUtils.findStringByRegex(message, Pattern.compile("following code: .*"));

            client.getSession().send(new ClientChatPacket(line.split(" ")[2]));
            client.register();

            Statistics.solvedCaptcha.getAndIncrement();
        } catch (Exception ignored) {}

        // clique na cor ******* | Captcha

        try {
            String line = FindUtils.findStringByRegex(message, Pattern.compile("clique na cor .*"));

            client.getSession().send(new ClientChatPacket("/color " + line.split(" ")[3]));
            client.register();

            Statistics.solvedCaptcha.getAndIncrement();
        } catch (Exception ignored) {}
    }
}
