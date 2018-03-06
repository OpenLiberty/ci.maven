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
package ${package}.it;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

public class GreetingIT {
    private static String URL;

    @BeforeClass
    public static void init() {
        String port = System.getProperty("liberty.test.port");
        URL = "http://localhost:" + port + "/${artifactId}/greeting/hello/JDoe";
    }

    @Test
    public void testService() throws Exception {

        Client client = null;
        WebTarget target = null;
        try {
            client = ClientBuilder.newClient();
            target = client.target(URL);

        } catch (Exception e) {
            client.close();
            throw e;
        }

        Response response = target.request().get();
        try {
            assertEquals("Response must be 200 OK", 200, response.getStatus());

            if (response == null) {
                assertNotNull("GreetingService response must not be NULL", response);
            } else {
                String respStr = response.readEntity(String.class);
                assertTrue("Response must contain \"Hello\"", respStr.contains("Hello"));
            }

        } finally {
            response.close();
        }
    }
}
