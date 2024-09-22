package pl.com.witia.fsp.protocol;

import java.nio.charset.Charset;

import pl.com.witia.fsp.errors.IncorrectCommandError;

public class Version {

    protected String serverVersion;

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public static Version from(Message msg) throws IncorrectCommandError {
        if (msg.getHeader().getCommand() != Header.CC_VERSION)
            throw new IncorrectCommandError();

        Version version = new Version();

        version.serverVersion = new String(msg.getData(), Charset.forName("windows-1250"));

        return version;
    }

}

