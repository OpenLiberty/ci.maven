// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
package it.io.openliberty.guides.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jakarta.json.JsonObject;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;


public class EndpointIT extends BaseDevTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
       setUpBeforeClass(null, "../resources/basic-dev-project", true, false, null, null);
 
       File f = new File("liberty/wlp");
       assertTrue(f.getCanonicalFile() + " exists", f.exists());
    }
 
    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
       BaseDevTest.cleanUpAfterClass();
    }

    @Test
    public void testGetProperties() {
        String port = System.getProperty("liberty.test.port");
        String url = "http://localhost:" + port + "/";

        Client client = ClientBuilder.newClient();

        WebTarget target = client.target(url + "system/properties-new");
        Response response = target.request().get();
        JsonObject obj = response.readEntity(JsonObject.class);

        assertEquals(200, response.getStatus(), "Incorrect response code from " + url);

        assertEquals("/opt/ol/wlp/output/defaultServer/",
                     obj.getString("server.output.dir"),
                     "The system property for the server output directory should match "
                     + "the Open Liberty container image.");

        response.close();
    }
}
