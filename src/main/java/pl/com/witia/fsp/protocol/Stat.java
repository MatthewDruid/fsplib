package pl.com.witia.fsp.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;

import pl.com.witia.fsp.errors.IncorrectCommandError;

public class Stat {

    protected Instant time;
    protected int size;
    protected byte type;

    protected Stat() {
    }

    public Instant getTime() {
        return time;
    }

    public int getSize() {
        return size;
    }

    public byte getType() {
        return type;
    }

    public static Stat from(Message msg) throws IncorrectCommandError {
        if (msg.getHeader().getCommand() != Header.CC_STAT)
            throw new IncorrectCommandError();

        ByteBuffer stat_buff = ByteBuffer.wrap(msg.getData());
        stat_buff.order(ByteOrder.BIG_ENDIAN);

        Stat stat = new Stat();

        stat.time = Instant.ofEpochSecond(stat_buff.getInt());
        stat.size = stat_buff.getInt();
        stat.type = stat_buff.get();

        return stat;
    }

}

