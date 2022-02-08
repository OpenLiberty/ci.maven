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
// tag::manager[]
package io.openliberty.guides.inventory;

import java.net.URL;
import java.net.UnknownHostException;
import java.net.MalformedURLException;
import javax.ws.rs.ProcessingException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.Properties;
import javax.inject.Inject;
import javax.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import io.openliberty.guides.inventory.model.InventoryList;
import io.openliberty.guides.inventory.model.SystemData;
import io.openliberty.guides.inventory.api.DefaultApi;
import io.openliberty.guides.inventory.api.ApiException;
import io.openliberty.guides.inventory.api.ApiExceptionMapper;

@ApplicationScoped
public class InventoryManager {

  private List<SystemData> systems = Collections.synchronizedList(new ArrayList<>());
  private final String DEFAULT_PORT = System.getProperty("default.http.port");

  @Inject
  @RestClient
  private DefaultApi defaultRestClient;

  public Properties get(String hostname) {
    Properties properties = null;
    if (hostname.equals("localhost")) {
      properties = getPropertiesWithDefaultHostName();
    } else {
      properties = getPropertiesWithGivenHostName(hostname);
    }

    return properties;
  }

  public void add(String hostname, Properties systemProps) {
    Properties props = new Properties();
    props.setProperty("os.name", systemProps.getProperty("os.name"));
    props.setProperty("user.name", systemProps.getProperty("user.name"));

    SystemData host = new SystemData();
    host.setHostname(hostname);
    host.setProperties((Map)props);
    if (!systems.contains(host))
      systems.add(host);
  }

  public InventoryList list() {
    InventoryList list = new InventoryList();
    list.setSystems(systems);
    list.setTotal(systems.size());
    return list;
  }

  private Properties getPropertiesWithDefaultHostName() {
    try {
      Properties properties = new Properties();
      properties.putAll(defaultRestClient.getProperties());
      return properties;
    } catch (ApiException e) {
      System.err.println("The given URL is unreachable.");
    } catch (ProcessingException ex) {
      handleProcessingException(ex);
    }
    return null;
  }

  // tag::builder[]
  private Properties getPropertiesWithGivenHostName(String hostname) {
    String customURLString = "http://" + hostname + ":" + DEFAULT_PORT + "/system";
    URL customURL = null;
    try {
      customURL = new URL(customURLString);
      DefaultApi customRestClient = RestClientBuilder.newBuilder()
                                         .baseUrl(customURL)
                                         .register(ApiExceptionMapper.class)
                                         .build(DefaultApi.class);
      Properties properties = new Properties();
      properties.putAll(customRestClient.getProperties());
      return properties;
    } catch (ProcessingException ex) {
      handleProcessingException(ex);
    } catch (ApiException e) {
      System.err.println("The given URL is unreachable.");
    } catch (MalformedURLException e) {
      System.err.println("The given URL is not formatted correctly.");
    }
    return null;
  }
  // end::builder[]

  private void handleProcessingException(ProcessingException ex) {
    Throwable rootEx = ExceptionUtils.getRootCause(ex);
    if (rootEx != null && rootEx instanceof UnknownHostException) {
      System.err.println("The specified host is unknown.");
    } else {
      throw ex;
    }
  }

}
// end::manager[]
