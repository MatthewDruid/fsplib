package pl.com.witia.fsp.protocol;

public class DirEntry {

    public static final int HEAD_LEN = 9;

    protected int time;
    
    protected int size;

    protected byte type;

    protected String name;

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}

