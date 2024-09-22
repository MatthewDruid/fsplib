package pl.com.witia.fsp;

import java.io.IOException;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class ClientTests {

    protected static Client client = null;

    @BeforeAll
    public static void beforeAll() throws IOException {
        client = new Client(Executors.newSingleThreadExecutor());
    }

    @AfterAll
    public static void afterAll() throws IOException {
        if (client instanceof Client)
            client.close();
    }

}

