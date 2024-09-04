package pl.com.witia.fsp.errors;

public abstract class FSPException extends Exception {

    public FSPException() {
        super();
    }

    public FSPException(String msg) {
        super(msg);
    }

}

