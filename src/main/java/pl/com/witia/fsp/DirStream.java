package pl.com.witia.fsp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import pl.com.witia.fsp.errors.FSPError;
import pl.com.witia.fsp.errors.IncorrectCommandError;
import pl.com.witia.fsp.errors.IncorrectSequenceError;
import pl.com.witia.fsp.protocol.DirEntry;
import pl.com.witia.fsp.protocol.Header;
import pl.com.witia.fsp.protocol.Message;

public class DirStream implements Iterable<DirEntry> {

    protected final ReadWriteLock entriesLock = new ReentrantReadWriteLock();
    protected final List<DirEntry> entries;

    private class DirIterator implements Iterator<DirEntry> {

        protected int index = 0;

        @Override
        public boolean hasNext() {
            if (index == entries.size())
                return false;

            return true;
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

    private DirStream(
        List<DirEntry> entries
    ) {
        this.entries = entries;
    }

    @Override
    public Iterator<DirEntry> iterator() {
        return new DirIterator();
    }

    protected static boolean process(Message msg, List<DirEntry> entries) {
        byte[] msg_data = msg.getData();

        ByteBuffer dir_buff = ByteBuffer.wrap(msg_data);
        dir_buff.order(ByteOrder.BIG_ENDIAN);

        while (dir_buff.position() < dir_buff.limit()) {
            DirEntry entry = new DirEntry();
            entry.setTime(dir_buff.getInt());
            entry.setSize(dir_buff.getInt());
            entry.setType(dir_buff.get());

            if (entry.getType() == (byte)0x2a) {
                return true;
            }

            if (entry.getType() == (byte)0x00) {
                break;
            }

            dir_buff.mark();
            int name_len = 0;
            for (byte c = dir_buff.get(); c != 0; c = dir_buff.get())
                name_len += 1;
            dir_buff.reset();
            byte[] name = new byte[name_len];
            dir_buff.get(name);
            dir_buff.get();

            entry.setName(Utils.butesToString(name));

            entries.add(entry);

            int len = name_len + 1 + DirEntry.HEAD_LEN;
            for (int i = (4 - (len % 4)) % 4; i > 0; i -= 1)
                dir_buff.get();

            if (dir_buff.limit() - dir_buff.position() < DirEntry.HEAD_LEN)
                break;
        }

        return false;
    }

    protected static Message get(Session session, byte[] data, int position)
            throws IOException, InterruptedException, FSPError, IncorrectSequenceError, IncorrectCommandError {
        short sequence = (short)((int)Math.random() & 0xffff);

        Header hdr = new Header();
        hdr.setCommand(Header.CC_GET_DIR);
        hdr.setFilePosition(position);
        hdr.setSequence(sequence);

        Message msg = new Message();
        msg.setHeader(hdr);
        msg.setData(data);

        msg = session.send(session.dest, msg);
        if (sequence != msg.getHeader().getSequence())
            throw new IncorrectSequenceError();

        if (msg.getHeader().getCommand() != Header.CC_GET_DIR)
            throw new IncorrectCommandError(String.format("Expected 0x%02x but is 0x%02x", Header.CC_GET_DIR, msg.getHeader().getCommand()));

        return msg;
    }

    protected static DirStream init(
        Session session,
        String path
    ) throws IOException, InterruptedException, FSPError, IncorrectSequenceError, IncorrectCommandError {
        ArrayList<DirEntry> entries = new ArrayList<>();
        byte[] asciiz = Utils.stringToASCIIZ(path);

        Message msg = null;
        int position = 0;
        do {
            msg = get(session, asciiz, position);
            position += 1;
        } while (process(msg, entries));

        return new DirStream(entries);
    }

}

