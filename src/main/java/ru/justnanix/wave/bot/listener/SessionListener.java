package ru.justnanix.wave.bot.listener;

import com.github.steveice10.mc.protocol.data.game.ClientRequest;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientRequestPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientTeleportConfirmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import org.apache.commons.lang3.StringUtils;
import ru.justnanix.wave.Wave;
import ru.justnanix.wave.bot.Bot;
import ru.justnanix.wave.utils.*;

import java.util.concurrent.atomic.AtomicInteger;

public class SessionListener extends SessionAdapter {
    private final Bot client;
    private int disconnects = 0;

    public SessionListener(Bot client) {
        this.client = client;
        // Движение запускается только после ServerJoinGamePacket — см. packetReceived
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        Statistics.botList.remove(client);

        if (Options.infoFormat < 1)
            Logger.error(client.getHost() + ":" + client.getPort()
                    + "  " + client.getGameProfile().getName()
                    + "  дисконнект: " + event.getReason());

        if (Options.doubleJoin)
            disconnects++;

        if (!event.getReason().equals("bf") && disconnects < 2 && Options.doubleJoin) {
            ThreadUtils.sleep(5000L);
            client.connect();
        }
    }

    @Override
    public void packetReceived(PacketReceivedEvent receiveEvent) {
        if (receiveEvent.getPacket() instanceof ServerJoinGamePacket) {
            if (Options.infoFormat < 1)
                Logger.bot(
                    client.getSession().getHost() + ":" + client.getSession().getPort(),
                    client.getGameProfile().getName(),
                    "подключился"
                );

            Statistics.botList.add(client);

            // Регистрация с задержкой
            new Thread(client::register).start();

            // Движение — запускаем только после join
            if (Options.move) {
                new Thread(() -> {
                    double posX = -0.2;
                    double posZ = -0.2;
                    while (client.isOnline()) {
                        for (int i = 0; i < Wave.getInstance().getRandom().nextInt(200); i++) {
                            if (!client.isOnline()) return;
                            client.getSession().send(new ClientPlayerPositionPacket(true, client.getPosX() + posX, client.getPosY(), client.getPosZ()));
                            client.setPosX(client.getPosX() + posX);
                            ThreadUtils.sleep(50L);
                        }
                        for (int i = 0; i < Wave.getInstance().getRandom().nextInt(200); i++) {
                            if (!client.isOnline()) return;
                            client.getSession().send(new ClientPlayerPositionPacket(true, client.getPosX(), client.getPosY(), client.getPosZ() + posZ));
                            client.setPosZ(client.getPosZ() + posZ);
                            ThreadUtils.sleep(50L);
                        }
                        posX = -posX;
                        posZ = -posZ;
                    }
                }).start();
            }

            // Команды — с задержкой после регистрации
            new Thread(() -> {
                ThreadUtils.sleep(3000L); // ждём регистрацию/логин
                while (client.isOnline()) {
                    for (Object obj : Options.commands) {
                        if (!client.isOnline()) break;
                        try {
                            CommandParser.parseCommand((String) obj, client);
                        } catch (Exception ignored) {}
                        ThreadUtils.sleep(Options.commandDelay);
                    }
                    ThreadUtils.sleep(Options.commandLoopDelay);
                }
            }).start();
        }  else if (receiveEvent.getPacket() instanceof ServerChatPacket) {
            ServerChatPacket packet = receiveEvent.getPacket();
            String message = packet.getMessage().getFullText();

            if (Options.antiBotFilter && message.contains("Ожидайте завершения проверки...")) {
                String ip = client.getSession().getHost() + ":" + client.getSession().getPort();

                if (Options.infoFormat < 1)
                    Logger.antiBotFilter(ip);

                Wave.getInstance().getServerParser().getServers().remove(ip);
                client.getSession().disconnect("bf");
                return;
            }

            if (!StringUtils.containsIgnoreCase(message, "chat.type") && Options.infoFormat < 1)
                Logger.chat(
                    client.getSession().getHost() + ":" + client.getSession().getPort(),
                    client.getGameProfile().getName(),
                    message
                );

            CaptchaUtils.solveCaptcha(message, client);
        } else if (receiveEvent.getPacket() instanceof ServerPlayerPositionRotationPacket) {
            ServerPlayerPositionRotationPacket packet = receiveEvent.getPacket();

            client.setPosX(packet.getX());
            client.setPosY(packet.getY());
            client.setPosZ(packet.getZ());

            client.getSession().send(new ClientTeleportConfirmPacket(packet.getTeleportId()));
        } else if (receiveEvent.getPacket() instanceof ServerPlayerHealthPacket) {
            if (((ServerPlayerHealthPacket) receiveEvent.getPacket()).getHealth() < 1) {
                client.getSession().send(new ClientRequestPacket(ClientRequest.RESPAWN));
            }
        }
    }
}
