/*******************************************************************************
 * (c) Copyright IBM Corporation 2017.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package wasdev.ejb.it;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class EndpointIT {
    private static String URL;

    @BeforeClass
    public static void init() {
        URL = "http://localhost:9080/ejb-war";
    }

    @Test
    public void testServlet() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet method = new HttpGet(URL);

            try (CloseableHttpResponse response = client.execute(method)) {
                int statusCode = response.getStatusLine().getStatusCode();

                assertEquals("HTTP GET failed", HttpStatus.SC_OK, statusCode);

                String responseBody = EntityUtils.toString(response.getEntity());

                assertTrue("Unexpected response body", responseBody.contains("Hello EJB World."));
            }
        }
    }
}
