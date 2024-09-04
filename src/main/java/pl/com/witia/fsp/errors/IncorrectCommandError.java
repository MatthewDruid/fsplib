package pl.com.witia.fsp.errors;

public class IncorrectCommandError extends FSPException {

    public IncorrectCommandError() {
        super();
    }

    public IncorrectCommandError(String message) {
        super(message);
    }

}

