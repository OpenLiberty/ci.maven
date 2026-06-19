package application;

import static org.junit.Assert.*;
import org.junit.Test;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SpringBoot40ServerXmlNodeRestEndpointIT {
    
    @Test
    public void testHealthEndpoint() throws Exception {
        URL url = new URL("http://localhost:9080/health");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        int responseCode = conn.getResponseCode();
        assertEquals("Health endpoint should return 200", 200, responseCode);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String response = in.readLine();
        in.close();
        
        assertTrue("Response should contain expected message", 
                   response.contains("Spring Boot 4.0 with server.xml springBoot node configuration is running!"));
    }
}
