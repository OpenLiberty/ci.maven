// tag::comment[]
/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::comment[]
// tag::HealthTestUtil[]
package it.io.openliberty.guides.health;

import static org.junit.Assert.assertEquals;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.provider.jsrjsonp.JsrJsonpProvider;

public class HealthTestUtil {

  private static String port;
  private static String baseUrl;
  private final static String HEALTH_ENDPOINT = "health";
  public static final String INV_MAINTENANCE_FALSE = "io_openliberty_guides_inventory_inMaintenance\":false";
  public static final String INV_MAINTENANCE_TRUE = "io_openliberty_guides_inventory_inMaintenance\":true";

  static {
    port = System.getProperty("liberty.test.port");
    baseUrl = "http://localhost:" + port + "/";
  }

  public static JsonArray connectToHealthEnpoint(int expectedResponseCode) {
    String healthURL = baseUrl + HEALTH_ENDPOINT;
    Client client = ClientBuilder.newClient().register(JsrJsonpProvider.class);
    Response response = client.target(healthURL).request().get();
    assertEquals("Response code is not matching " + healthURL,
                 expectedResponseCode, response.getStatus());
    JsonArray servicesStates = response.readEntity(JsonObject.class)
                                       .getJsonArray("checks");
    response.close();
    client.close();
    return servicesStates;
  }

  public static String getActualState(String service,
      JsonArray servicesStates) {
    String state = "";
    for (Object obj : servicesStates) {
      if (obj instanceof JsonObject) {
        if (service.equals(((JsonObject) obj).getString("name"))) {
          state = ((JsonObject) obj).getString("state");
        }
      }
    }
    return state;
  }

  public static void changeInventoryProperty(String oldValue, String newValue) {
    try {
      String fileName = System.getProperty("user.dir").split("target")[0]
          + "/resources/CustomConfigSource.json";
      BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
      String line = "";
      String oldContent = "", newContent = "";
      while ((line = reader.readLine()) != null) {
        oldContent += line + "\r\n";
      }
      reader.close();
      newContent = oldContent.replaceAll(oldValue, newValue);
      FileWriter writer = new FileWriter(fileName);
      writer.write(newContent);
      writer.close();
      Thread.sleep(600);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void cleanUp() {
    changeInventoryProperty(INV_MAINTENANCE_TRUE, INV_MAINTENANCE_FALSE);
  }

}
// end::HealthTestUtil[]
