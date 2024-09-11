package pl.com.witia.fsp;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client implements Closeable {

    private static final Logger logger = Logger.getLogger(Client.class.getName());

    protected final ConcurrentHashMap<InetSocketAddress, Session> sessions = new ConcurrentHashMap<>();
    protected final ExecutorService executor;
    protected SocketAddress local = null;

    public Client(ExecutorService executor) throws IOException {
        this.executor = executor;
    }

    public Client bind(SocketAddress local) throws IOException {
        this.local = local;
        return this;
    }

    public Session session(String host, int port) throws Exception {
        InetSocketAddress address = new InetSocketAddress(host, port);

        if (sessions.containsKey(address))
            return sessions.get(address);

        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.bind(local);

        Session session = new Session(channel, address, executor);
        sessions.put(address, session);

        return session;
    }

    @Override
    public void close() throws IOException {
        for (Session session: sessions.values()) {
            try {
                session.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
        }
    }

}

