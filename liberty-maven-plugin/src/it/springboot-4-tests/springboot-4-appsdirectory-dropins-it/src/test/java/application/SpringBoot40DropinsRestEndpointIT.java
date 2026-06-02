package application;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Test;

public class SpringBoot40DropinsRestEndpointIT {

    @Test
    public void testSpringRestBootEndpoint() throws Exception {

        URL requestUrl = new URL("http://localhost:9080/");
        HttpURLConnection conn = (HttpURLConnection) requestUrl.openConnection();

        if (conn != null) {
            assertEquals("Expected response code not found.", 200, conn.getResponseCode());
        }

        StringBuffer response = new StringBuffer();

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
        }
        assertEquals("Expected body not found.", "HELLO SPRING BOOT 4.0!!", response.toString());
    }
}
