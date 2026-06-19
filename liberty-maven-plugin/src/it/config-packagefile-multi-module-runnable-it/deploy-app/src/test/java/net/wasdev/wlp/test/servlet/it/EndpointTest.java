package net.wasdev.wlp.test.servlet.it;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class EndpointTest {
    private static String URL;

    @BeforeClass
    public static void init() {
        URL = "http://localhost:9080/deploy-app/servlet";
    }

    @Test
    public void testServlet() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet method = new HttpGet(URL);

            try (CloseableHttpResponse response = client.execute(method)) {
                int statusCode = response.getStatusLine().getStatusCode();

                assertEquals("HTTP GET failed", HttpStatus.SC_OK, statusCode);

                String responseBody = EntityUtils.toString(response.getEntity());

                assertTrue("Unexpected response body", responseBody.contains("Hello! How are you today?"));
            }
        }
    }
}
