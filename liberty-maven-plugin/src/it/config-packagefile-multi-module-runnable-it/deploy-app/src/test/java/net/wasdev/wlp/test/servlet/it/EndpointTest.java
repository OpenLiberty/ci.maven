package net.wasdev.wlp.test.servlet.it;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

public class EndpointTest {
    private static String URL;

    @BeforeClass
    public static void init() {
        URL = "http://localhost:9080/deploy-app/servlet";
    }

    @Test
    public void testServlet() throws Exception {
        HttpClient client = new HttpClient();

        GetMethod method = new GetMethod(URL);

        try {
            int statusCode = client.executeMethod(method);

            assertEquals("HTTP GET failed", HttpStatus.SC_OK, statusCode);

            String response = method.getResponseBodyAsString();

            assertTrue("Unexpected response body", response.contains("Hello! How are you today?"));
        } finally {
            method.releaseConnection();
        }  
    }
}
