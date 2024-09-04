package pl.com.witia.fsp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import pl.com.witia.fsp.errors.FSPError;
import pl.com.witia.fsp.errors.IncorrectCommandError;
import pl.com.witia.fsp.errors.IncorrectSequenceError;
import pl.com.witia.fsp.protocol.DirEntry;
import pl.com.witia.fsp.protocol.Header;
import pl.com.witia.fsp.protocol.Message;

public class DirStream implements Iterable<DirEntry> {

    protected final Session session;
    protected final String path;
    protected final byte[] asciiz;

    protected final ReadWriteLock entriesLock = new ReentrantReadWriteLock();
    protected final LinkedList<DirEntry> entries = new LinkedList<>();

    protected volatile int position = 0;
    protected volatile boolean lastLoaded = false;

    private class DirIterator implements Iterator<DirEntry> {

        protected int index = 0;

        @Override
        public boolean hasNext() {
            try {
                entriesLock.readLock().lock();

                if (lastLoaded) {
                    if (index == entries.size())
                        return false;

                    return true;
                }

                if (index >= entries.size() - 2) {
                    try {
                        getNext();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                return true;
            } finally {
                entriesLock.readLock().unlock();
            }
        }

        @Override
        public DirEntry next() {
            try {
                entriesLock.readLock().lock();

                DirEntry entry = entries.get(index);
                index += 1;
                return entry;
            } finally {
                entriesLock.readLock().unlock();
            }
        }
    }

    protected DirStream(Session session, String path) throws IncorrectSequenceError, IncorrectCommandError, IOException, InterruptedException, FSPError {
        this.session = session;
        this.path = path;
        this.asciiz = Utils.stringToASCIIZ(path);

        getNext();
    }

    @Override
    public Iterator<DirEntry> iterator() {
        return new DirIterator();
    }

    private void process(Message msg) {
        try {
            entriesLock.writeLock().lock();

            byte[] msg_data = msg.getData();

            ByteBuffer dir_buff = ByteBuffer.wrap(msg_data);
            dir_buff.order(ByteOrder.BIG_ENDIAN);

            while (dir_buff.position() < dir_buff.limit()) {
                DirEntry entry = new DirEntry();
                entry.setTime(dir_buff.getInt());
                entry.setSize(dir_buff.getInt());
                entry.setType(dir_buff.get());

                dir_buff.mark();
                int name_len = 0;
                for (byte c = dir_buff.get(); c != 0; c = dir_buff.get())
                    name_len += 1;
                dir_buff.reset();
                byte[] name = new byte[name_len];
                dir_buff.get(name);
                dir_buff.get();

                entry.setName(Utils.butesToString(name));

                if (entry.getType() == (byte)0x00) {
                    lastLoaded = true;
                    break;
                }

                entries.add(entry);

                int len = name_len + 1 + DirEntry.HEAD_LEN;
                for (int i = 4 - (len % 4); i < 4 && i > 0; i -= 1)
                    dir_buff.get();

                if (dir_buff.limit() - dir_buff.position() < DirEntry.HEAD_LEN)
                    break;
            }
        } finally {
            entriesLock.writeLock().unlock();
        }
    }

    protected void getNext(
    ) throws IOException, InterruptedException, FSPError, IncorrectSequenceError, IncorrectCommandError {
        if (lastLoaded)
            return;

        short sequence = (short)((int)Math.random() & 0xffff);

        Header hdr = new Header();
        hdr.setCommand(Header.CC_GET_DIR);
        hdr.setFilePosition(position);
        hdr.setSequence(sequence);

        Message msg = new Message();
        msg.setHeader(hdr);
        msg.setData(asciiz);

        msg = session.send(session.dest, msg);
        if (sequence != msg.getHeader().getSequence())
            throw new IncorrectSequenceError();

        if (msg.getHeader().getCommand() != Header.CC_GET_DIR)
            throw new IncorrectCommandError(String.format("Expected 0x%02x but is 0x%02x", Header.CC_GET_DIR, msg.getHeader().getCommand()));

        if (lastLoaded)
            return;

        process(msg);

        position += 1;
    }

}

