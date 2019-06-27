// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
// tag::testClass[]
package it.io.openliberty.guides.inventory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.provider.jsrjsonp.JsrJsonpProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class InventoryEndpointTest {

  private static String port;
  private static String baseUrl;

  private Client client;

  private final String SYSTEM_PROPERTIES = "system/properties";
  private final String INVENTORY_SYSTEMS = "inventory/systems";

  @BeforeClass
  public static void oneTimeSetup() {
    port = System.getProperty("liberty.test.port");
    baseUrl = "http://localhost:" + port + "/";
  }

  @Before
  public void setup() {
    client = ClientBuilder.newClient();
    client.register(JsrJsonpProvider.class);
  }

  @After
  public void teardown() {
    client.close();
  }

  // tag::tests[]
  // tag::testSuite[]
  @Test
  public void testSuite() {
    //this.testEmptyInventory();
    this.testHostRegistration();
    this.testSystemPropertiesMatch();
    this.testUnknownHost();
  }
  // end::testSuite[]

  // tag::testEmptyInventory[]
  // TODO: temporarily disabled to allow tests to be re-run on the same server 
  /*public void testEmptyInventory() {
    Response response = this.getResponse(baseUrl + INVENTORY_SYSTEMS);
    this.assertResponse(baseUrl, response);

    JsonObject obj = response.readEntity(JsonObject.class);

    int expected = 0;
    int actual = obj.getInt("total");
    assertEquals("The inventory should be empty on application start but it wasn't",
                 expected, actual);

    response.close();
  }*/
  // end::testEmptyInventory[]

  // tag::testHostRegistration[]
  public void testHostRegistration() {
    this.visitLocalhost();

    Response response = this.getResponse(baseUrl + INVENTORY_SYSTEMS);
    this.assertResponse(baseUrl, response);

    JsonObject obj = response.readEntity(JsonObject.class);

    int expected = 1;
    int actual = obj.getInt("total");
    assertEquals("The inventory should have one entry for localhost", expected,
                 actual);
    boolean localhostExists = obj.getJsonArray("systems").getJsonObject(0)
                                 .get("hostname").toString()
                                 .contains("localhost");
    assertTrue("A host was registered, but it was not localhost",
               localhostExists);

    response.close();
  }
  // end::testHostRegistration[]

  // tag::testSystemPropertiesMatch[]
  public void testSystemPropertiesMatch() {
    Response invResponse = this.getResponse(baseUrl + INVENTORY_SYSTEMS);
    Response sysResponse = this.getResponse(baseUrl + SYSTEM_PROPERTIES);

    this.assertResponse(baseUrl, invResponse);
    this.assertResponse(baseUrl, sysResponse);

    JsonObject jsonFromInventory = (JsonObject) invResponse.readEntity(JsonObject.class)
                                                           .getJsonArray("systems")
                                                           .getJsonObject(0)
                                                           .get("properties");

    JsonObject jsonFromSystem = sysResponse.readEntity(JsonObject.class);

    String osNameFromInventory = jsonFromInventory.getString("os.name");
    String osNameFromSystem = jsonFromSystem.getString("os.name");
    this.assertProperty("os.name", "localhost", osNameFromSystem,
                        osNameFromInventory);

    String userNameFromInventory = jsonFromInventory.getString("user.name");
    String userNameFromSystem = jsonFromSystem.getString("user.name");
    this.assertProperty("user.name", "localhost", userNameFromSystem,
                        userNameFromInventory);

    invResponse.close();
    sysResponse.close();
  }
  // end::testSystemPropertiesMatch[]

  // tag::testUnknownHost[]
  public void testUnknownHost() {
    Response response = this.getResponse(baseUrl + INVENTORY_SYSTEMS);
    this.assertResponse(baseUrl, response);

    Response badResponse = client.target(baseUrl + INVENTORY_SYSTEMS + "/"
        + "badhostname").request(MediaType.APPLICATION_JSON).get();

    String obj = badResponse.readEntity(String.class);

    boolean isError = obj.contains("ERROR");
    assertTrue("badhostname is not a valid host but it didn't raise an error",
               isError);

    response.close();
    badResponse.close();
  }

  // end::testUnknownHost[]
  // end::tests[]
  // tag::helpers[]
  // tag::javadoc[]
  /**
   * <p>
   * Returns response information from the specified URL.
   * </p>
   *
   * @param url
   *          - target URL.
   * @return Response object with the response from the specified URL.
   */
  // end::javadoc[]
  private Response getResponse(String url) {
    return client.target(url).request().get();
  }

  // tag::javadoc[]
  /**
   * <p>
   * Asserts that the given URL has the correct response code of 200.
   * </p>
   *
   * @param url
   *          - target URL.
   * @param response
   *          - response received from the target URL.
   */
  // end::javadoc[]
  private void assertResponse(String url, Response response) {
    assertEquals("Incorrect response code from " + url, 200,
                 response.getStatus());
  }

  // tag::javadoc[]
  /**
   * Asserts that the specified JVM system property is equivalent in both the
   * system and inventory services.
   *
   * @param propertyName
   *          - name of the system property to check.
   * @param hostname
   *          - name of JVM's host.
   * @param expected
   *          - expected name.
   * @param actual
   *          - actual name.
   */
  // end::javadoc[]
  private void assertProperty(String propertyName, String hostname,
      String expected, String actual) {
    assertEquals("JVM system property [" + propertyName + "] "
        + "in the system service does not match the one stored in "
        + "the inventory service for " + hostname, expected, actual);
  }

  // tag::javadoc[]
  /**
   * Makes a simple GET request to inventory/localhost.
   */
  // end::javadoc[]
  private void visitLocalhost() {
    Response response = this.getResponse(baseUrl + SYSTEM_PROPERTIES);
    this.assertResponse(baseUrl, response);
    response.close();

    Response targetResponse = client.target(baseUrl + INVENTORY_SYSTEMS
        + "/localhost").request().get();
    targetResponse.close();
  }
  // end::helpers[]
}
// end::testClass[]
