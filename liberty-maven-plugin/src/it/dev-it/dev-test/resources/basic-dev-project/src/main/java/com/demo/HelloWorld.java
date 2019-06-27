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
package com.demo;

// import javax.enterprise.context.RequestScoped;
// import javax.ws.rs.GET;
// import javax.ws.rs.core.Response;
// import javax.ws.rs.Path;
// import javax.ws.rs.Produces;
// import javax.ws.rs.core.MediaType;

// @RequestScoped
// @Path("properties")
public class HelloWorld {

	public String helloWorld() {
		return "helloWorld";
	}

  // @GET
  // @Produces(MediaType.APPLICATION_JSON)
  // public Response getProperties() {
  //   return Response.ok(System.getProperties()).build();
  // }
}
