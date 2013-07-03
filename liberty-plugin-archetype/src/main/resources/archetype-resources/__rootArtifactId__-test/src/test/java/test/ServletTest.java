package ${package}.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

public class ServletTest {

    private static String URL;

    @BeforeClass
    public static void init() {
        String contextPath = System.getProperty("contextPath", "/SimpleServlet");
        String httpPort = System.getProperty("httpPort", "9080");
        URL = "http://localhost:" + httpPort + contextPath + "/SimpleServlet";
    }

    @Test
    public void testServlet() throws Exception {
        HttpClient client = new HttpClient();

        GetMethod method = new GetMethod(URL);

        try {
            int statusCode = client.executeMethod(method);

            assertEquals("HTTP GET failed", HttpStatus.SC_OK, statusCode);

            String response = method.getResponseBodyAsString();

            assertTrue("Unexpected response body", response.contains("Simple Servlet ran successfully"));
        } finally {
            method.releaseConnection();
        }  
    }
    
}
