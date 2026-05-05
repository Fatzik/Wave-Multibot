package ru.justnanix.wave.bot;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.ProxyInfo;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import ru.justnanix.wave.Wave;
import ru.justnanix.wave.bot.listener.SessionListener;
import ru.justnanix.wave.parser.ProxyParser;
import ru.justnanix.wave.utils.Options;
import ru.justnanix.wave.utils.StringGenerator;
import ru.justnanix.wave.utils.ThreadUtils;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class Bot {
    private final MinecraftProtocol account;

    private final String host;
    private final int port;

    private Session session;
    private ProxyParser.ProxyEntry proxyEntry;

    private double posX;
    private double posY;
    private double posZ;

    public Bot(MinecraftProtocol account, String host, int port, ProxyParser.ProxyEntry proxyEntry) {
        this.account    = account;
        this.proxyEntry = proxyEntry;
        this.host       = host;
        this.port       = port;
    }

    public void connect() {
        ProxyInfo proxyInfo = null;
        if (proxyEntry != null) {
            InetSocketAddress addr = new InetSocketAddress(proxyEntry.host, proxyEntry.port);
            ProxyInfo.Type type;
            switch (proxyEntry.type) {
                case SOCKS4: type = ProxyInfo.Type.SOCKS4; break;
                case SOCKS5: type = ProxyInfo.Type.SOCKS5; break;
                default:     type = ProxyInfo.Type.HTTP;   break;
            }
            proxyInfo = new ProxyInfo(type, addr);
        }

        Client client = new Client(host, port, account, new TcpSessionFactory(proxyInfo));

        client.getSession().addListener(new SessionListener(this));
        client.getSession().connect();

        this.session = client.getSession();
    }

    public void register() {
        if (!isOnline())
            return;

        String password = Options.randomPasswords ? StringGenerator.generateStringInt(Options.randomPasswordsLength) : "4321qq4321";
        ThreadUtils.sleep(1000L);

        session.send(new ClientChatPacket(String.format("/register %s %1$s", password)));
        ThreadUtils.sleep(500L);
        session.send(new ClientChatPacket(String.format("/login %s", password)));
    }

    /** Отключается и переподключается с новым ником */
    public void reconnectWithNewNick() {
        String newNick = Options.randomNicks
                ? StringGenerator.generateNick(Options.randomNicksLength)
                : Wave.getInstance().getNicksParser().nextNick();

        if (Options.infoFormat < 1)
            ru.justnanix.wave.utils.Logger.bot(host + ":" + port, getGameProfile().getName(),
                    "смена ника → " + newNick);

        if (session != null && session.isConnected())
            session.disconnect("nick change");

        // Небольшая пауза перед переподключением
        ThreadUtils.sleep(1500L);

        // Создаём нового бота с новым ником и тем же прокси
        new Thread(() -> new Bot(
                new MinecraftProtocol(newNick), host, port, proxyEntry
        ).connect()).start();
    }

    public boolean isOnline() {
        return session != null && session.isConnected();
    }

    public Session getSession() {
        return session;
    }

    public GameProfile getGameProfile() {
        return account.getProfile();
    }

    public double getPosX() {
        return posX;
    }

    public void setPosX(double posX) {
        this.posX = posX;
    }

    public double getPosY() {
        return posY;
    }

    public void setPosY(double posY) {
        this.posY = posY;
    }

    public double getPosZ() {
        return posZ;
    }

    public void setPosZ(double posZ) {
        this.posZ = posZ;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
