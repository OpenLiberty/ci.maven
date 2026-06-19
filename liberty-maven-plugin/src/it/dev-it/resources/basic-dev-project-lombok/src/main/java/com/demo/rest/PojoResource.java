package com.demo.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/pojo")
public class PojoResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getPojo() {
        Pojo pojo = new Pojo("test");
        return "Pojo name: " + pojo.getName();
    }
}
