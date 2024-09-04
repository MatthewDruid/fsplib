package pl.com.witia.fsp.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Message implements Cloneable {

    protected Header header;

    protected byte[] data = null;

    protected byte[] xtraData = null;

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getXtraData() {
        return xtraData;
    }

    public void setXtraData(byte[] xtraData) {
        this.xtraData = xtraData;
    }

    public int size() {
        return Header.FSP_HEADER_SIZE
                + (data == null ? 0 : data.length)
                + (xtraData == null ? 0 : xtraData.length);
    }

    public ByteBuffer buffer() {
        ByteBuffer buf = ByteBuffer.allocate(size());
        buf.order(ByteOrder.BIG_ENDIAN);

        buf.put(header.array());
        if (data != null) buf.put(data);
        if (xtraData != null) buf.put(xtraData);

        buf.rewind();
        return buf;
    }

    public byte[] array() {
        return buffer().array();
    }

    public static byte checksum(ByteBuffer src) {
        short sum = 0;

        while (src.remaining() > 0)
            sum += (src.get() & 0xff);

        src.rewind();
        return (byte)(sum + (sum >>> 8));
    }

    public static byte checksum(byte[] src) {
        short sum = 0;

        for (byte d: src)
            sum += (d & 0xff); // (d & 0xff) act as unsigned conversion to short

        return (byte)(sum + (sum >>> 8));
    }

    public static Message from(ByteBuffer buff) {
        Message message = new Message();

        buff.order(ByteOrder.BIG_ENDIAN);

        ByteBuffer head = buff.slice(buff.position(), Header.FSP_HEADER_SIZE);
        buff.position(buff.position() + Header.FSP_HEADER_SIZE);

        message.header = Header.from(head);

        if (message.header.dataLength > 0) {
            byte[] data = new byte[message.header.dataLength];
            buff.get(data);
            message.data = data;
        }

        if (buff.remaining() > 0) {
            byte[] xtraData = new byte[buff.remaining()];
            buff.get(xtraData);
            message.xtraData = xtraData;
        }

        buff.rewind();
        return message;
    }

    public static Message from(byte[] src) {
        ByteBuffer buff = ByteBuffer.wrap(src);

        return from(buff);
    }

    @Override
    public Message clone() {
        try {
            Message clone = (Message) super.clone();

            clone.header = this.header.clone();

            if (this.data == null) clone.data = null;
            else clone.data = Arrays.copyOf(this.data, this.data.length);

            if (this.xtraData == null) clone.xtraData = null;
            else clone.xtraData = Arrays.copyOf(this.xtraData, this.xtraData.length);

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}

