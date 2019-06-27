// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]

// tag::config-class[]
package io.openliberty.guides.inventory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class InventoryConfig {

  // tag::port-number[]
  @Inject
  @ConfigProperty(name = "io_openliberty_guides_port_number")
  private int portNumber;
  // end::port-number[]

  // tag::build-in-converter[]
  @Inject
  @ConfigProperty(name = "io_openliberty_guides_inventory_inMaintenance")
  private Provider<Boolean> inMaintenance;
  // end::build-in-converter[]

  // tag::isInMaintenance[]
  public boolean isInMaintenance() {
    return inMaintenance.get();
  }
  // end::isInMaintenance[]

  // tag::getPortNumber[]
  public int getPortNumber() {
    return portNumber;
  }
  // end::getPortNumber[]
}
// end::config-class[]
