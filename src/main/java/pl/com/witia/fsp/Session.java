package pl.com.witia.fsp;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import pl.com.witia.fsp.errors.FSPError;
import pl.com.witia.fsp.errors.IncorrectCommandError;
import pl.com.witia.fsp.errors.IncorrectSequenceError;
import pl.com.witia.fsp.protocol.Header;
import pl.com.witia.fsp.protocol.Message;
import pl.com.witia.fsp.protocol.Stat;
import pl.com.witia.fsp.protocol.Version;

public class Session implements Closeable {

    protected final DatagramChannel channel;
    protected final InetSocketAddress dest;
    protected final ExecutorService executor;

    protected volatile short key = 0;

    protected Session(
        DatagramChannel channel,
        InetSocketAddress dest,
        ExecutorService executor
    ) {
        this.channel = channel;
        this.dest = dest;
        this.executor = executor;
    }

    public Future<Version> getServerVersion() {
        return executor.submit(() -> {
            short sequence = (short)((int)Math.random() & 0xffff);

            Header hdr = new Header();
            hdr.setCommand(Header.CC_VERSION);
            hdr.setSequence(sequence);

            Message msg = new Message();
            msg.setHeader(hdr);

            msg = send(dest, msg);
            if (sequence != msg.getHeader().getSequence())
                throw new IncorrectSequenceError();

            if (msg.getHeader().getCommand() != Header.CC_VERSION)
                throw new IncorrectCommandError(String.format("Expected 0x%02x but is 0x%02x", Header.CC_VERSION, msg.getHeader().getCommand()));

            return Version.from(msg);
        });
    }

    public Future<DirStream> getDirStream(String path) {
        return executor.submit(() -> {
            return DirStream.init(this, path);
        });
    }

    public Future<Stat> getEntryStat(String path) {
        return executor.submit(() -> {
            short sequence = (short)((int)Math.random() & 0xffff);

            Header hdr = new Header();
            hdr.setCommand(Header.CC_STAT);
            hdr.setSequence(sequence);

            Message msg = new Message();
            msg.setHeader(hdr);
            msg.setData(Utils.stringToASCIIZ(path));

            msg = send(dest, msg);
            if (sequence != msg.getHeader().getSequence())
                throw new IncorrectSequenceError();

            if (msg.getHeader().getCommand() != Header.CC_STAT)
                throw new IncorrectCommandError(String.format("Expected 0x%02x but is 0x%02x", Header.CC_VERSION, msg.getHeader().getCommand()));

            return Stat.from(msg);
        });
    }

    public Future<Path> getFile(String path) {
        return executor.submit(() -> {
            short sequence = (short)((int)Math.random() & 0xffff);

            Header hdr = new Header();
            hdr.setCommand(Header.CC_GET_FILE);
            hdr.setFilePosition(0);
            hdr.setSequence(sequence);

            Message msg = new Message();
            msg.setHeader(hdr);
            msg.setData(Utils.stringToASCIIZ(path));

            Path tmpFile = Files.createTempFile("FSP_TMP_", "");
            try (OutputStream out = Files.newOutputStream(tmpFile, StandardOpenOption.WRITE)) {
                Message resp = null;
                do {
                    resp = send(dest, msg);
                    if (sequence != resp.getHeader().getSequence())
                        throw new IncorrectSequenceError();

                    if (resp.getHeader().getCommand() != Header.CC_GET_FILE)
                        throw new IncorrectCommandError(String.format("Expected 0x%02x but is 0x%02x", Header.CC_VERSION, resp.getHeader().getCommand()));

                    if (resp.getData() == null)
                        break;

                    hdr.setFilePosition(hdr.getFilePosition() + resp.getData().length);

                    out.write(resp.getData());
                } while (resp.getData() != null);
            }

            return tmpFile;
        });
    }

    protected Message send(
        InetSocketAddress recipient,
        Message msg
    ) throws IOException, InterruptedException, FSPError {
        ByteBuffer recv = ByteBuffer.allocate(65535);
        InetSocketAddress sender = null;

        msg.getHeader().setKey(key);
        msg.getHeader().setChecksum((byte)msg.size());
        msg.getHeader().setChecksum(Message.checksum(msg.array()));

        _awaits: synchronized (channel) {
            for (int timeout = 1_340; timeout <= 60_000; timeout = (int)(timeout * 1.5)) {
                if (key != msg.getHeader().getKey()) {
                    msg.getHeader().setKey(key);
                    msg.getHeader().setChecksum((byte)msg.size());
                    msg.getHeader().setChecksum(Message.checksum(msg.array()));
                }

                channel.send(msg.buffer(), recipient);

                Thread.sleep(Duration.ofMillis(timeout));

                sender = (InetSocketAddress)channel.receive(recv);

                if (sender != null) {
                    break _awaits;
                }
            }

            throw new SocketTimeoutException();
        }

        recv.flip();

        Message m = Message.from(recv);
        recv.put(recv.position() + 1, (byte)0);

        if (m.getHeader().getChecksum() != Message.checksum(recv))
            throw new IOException("Incorrect data");

        if (key != m.getHeader().getKey())
            key = m.getHeader().getKey();

        if (m.getHeader().getCommand() == Header.CC_ERR)
            throw FSPError.from(m);

        return m;
    }

    @Override
    public void close() throws IOException {
        Header hdr = new Header();
        hdr.setCommand(Header.CC_BYE);

        Message msg = new Message();
        msg.setHeader(hdr);

        try {
            send(dest, msg);
        } catch (FSPError|InterruptedException e) {
        }

        channel.close();
    }

}

