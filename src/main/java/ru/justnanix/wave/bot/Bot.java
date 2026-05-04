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
    private Proxy proxy;

    private double posX;
    private double posY;
    private double posZ;

    public Bot(MinecraftProtocol account, String host, int port, Proxy proxy) {
        this.account = account;
        this.proxy = proxy;

        this.host = host;
        this.port = port;
    }

    public void connect() {
        ProxyInfo proxyInfo = null;
        if (proxy != null && proxy.type() != Proxy.Type.DIRECT) {
            InetSocketAddress addr = (InetSocketAddress) proxy.address();
            ProxyInfo.Type type = proxy.type() == Proxy.Type.SOCKS
                    ? ProxyInfo.Type.SOCKS5
                    : ProxyInfo.Type.HTTP;
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
