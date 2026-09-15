package io.openliberty.guides.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

@Path("properties")
public class PropertiesResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getProperties() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        System.getProperties()
              .entrySet()
              .stream()
              .forEach(entry -> builder.add((String) entry.getKey(), (String) entry.getValue()));
        return builder.build();
    }
}
