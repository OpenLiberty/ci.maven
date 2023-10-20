/**
 * (C) Copyright IBM Corporation 2023.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package application;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Test;

public class SpringBoot30RestEndpointIT {

    @Test
    public void testSpringRestBootEndpoint() throws Exception {

        URL requestUrl = new URL("http://localhost:9080/spring/");
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
        assertEquals("Expected body not found.", "HELLO SPRING BOOT!!", response.toString());
    }
}