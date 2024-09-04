package pl.com.witia.fsp.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Header implements Cloneable {

    public static final byte CC_VERSION     = (byte)0x10;
    public static final byte CC_ERR         = (byte)0x40;
    public static final byte CC_GET_DIR     = (byte)0x41;
    public static final byte CC_GET_FILE    = (byte)0x42;
    public static final byte CC_UP_LOAD     = (byte)0x43;
    public static final byte CC_INSTALL     = (byte)0x44;
    public static final byte CC_DEL_FILE    = (byte)0x45;
    public static final byte CC_DEL_DIR     = (byte)0x46;
    public static final byte CC_GET_PRO     = (byte)0x47;
    public static final byte CC_SET_PRO     = (byte)0x48;
    public static final byte CC_MAKE_DIR    = (byte)0x49;
    public static final byte CC_BYE         = (byte)0x4a;
    public static final byte CC_STAT        = (byte)0x4d; // Command starting from FSP 2.8.1 Beta 11

    public static final short FSP_HEADER_SIZE = 12;

    protected byte command;

    protected byte checksum;

    protected short key;

    protected short sequence;

    protected short dataLength;

    protected int filePosition;

    public byte getCommand() {
        return command;
    }

    public void setCommand(byte command) {
        this.command = command;
    }

    public byte getChecksum() {
        return checksum;
    }

    public void setChecksum(byte checksum) {
        this.checksum = checksum;
    }

    public short getKey() {
        return key;
    }

    public void setKey(short key) {
        this.key = key;
    }

    public short getSequence() {
        return sequence;
    }

    public void setSequence(short sequence) {
        this.sequence = sequence;
    }

    public short getDataLength() {
        return dataLength;
    }

    public void setDataLength(short dataLength) {
        this.dataLength = dataLength;
    }

    public int getFilePosition() {
        return filePosition;
    }

    public void setFilePosition(int filePosition) {
        this.filePosition = filePosition;
    }

    public ByteBuffer buffer() {
        ByteBuffer data = ByteBuffer.allocate(FSP_HEADER_SIZE);
        data.order(ByteOrder.BIG_ENDIAN);

        data
                .put(command)
                .put(checksum)
                .putShort(key)
                .putShort(sequence)
                .putShort(dataLength)
                .putInt(filePosition);

        data.rewind();
        return data;
    }

    public byte[] array() {
        return buffer().array();
    }

    public static Header from(ByteBuffer src) {
        Header header = new Header();

        src.order(ByteOrder.BIG_ENDIAN);

        header.command = src.get();
        header.checksum = src.get();
        header.key = src.getShort();
        header.sequence = src.getShort();
        header.dataLength = src.getShort();
        header.filePosition = src.getInt();

        src.rewind();
        return header;
    }

    public static Header from(byte[] src) {
        ByteBuffer buff = ByteBuffer.wrap(src);

        return from(buff);
    }

    @Override
    public Header clone() {
        try {
            Header clone = (Header) super.clone();

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}

