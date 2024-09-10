package pl.com.witia.fsp;

import java.nio.charset.Charset;
import java.util.Arrays;

public class Utils {

    protected static final Charset cs;

    static {
        String encoding = System.getProperty("fsp.encoding");
        cs = (encoding != null) ? Charset.forName(encoding) : Charset.defaultCharset();
    }

    public static byte[] stringToASCIIZ(String src) {
        byte[] src_buf = src.getBytes(cs);
        byte[] dst_buf = new byte[src_buf.length + 1];
        System.arraycopy(src_buf, 0, dst_buf, 0, src_buf.length);
        dst_buf[dst_buf.length - 1] = 0;
        return dst_buf;
    }

    public static String butesToString(byte[] src) {
        return new String(src, cs);
    }

    public static String ASCIIZToString(byte[] src) {
        byte[] src_buf = Arrays.copyOf(src, src.length);
        byte[] dst_buf = new byte[src_buf.length - 1];
        System.arraycopy(src_buf, 0, dst_buf, 0, dst_buf.length);
        return new String(dst_buf, cs);
    }

}

