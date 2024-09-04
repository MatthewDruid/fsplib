package pl.com.witia.fsp.errors;

import pl.com.witia.fsp.Utils;
import pl.com.witia.fsp.protocol.Message;

public class FSPError extends Exception {

    protected String message = "";
    protected short code = 0;

    protected FSPError() {
        super();
    }

    public String getMessage() {
        return message;
    }

    public short getCode() {
        return code;
    }

    public static FSPError from(Message msg) {
        FSPError err = new FSPError();

        err.message = Utils.butesToString(msg.getData());

        if (2 == msg.getHeader().getFilePosition()) {
            byte[] xtra = msg.getXtraData();
            if (2 == xtra.length)
                err.code = (short)(((xtra[0] & 0xff) << 8) | (xtra[1] & 0xff));
        }

        return err;
    }

}

