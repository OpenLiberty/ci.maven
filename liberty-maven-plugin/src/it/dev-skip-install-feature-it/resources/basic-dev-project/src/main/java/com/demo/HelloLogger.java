package com.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

@Path("/show-log")
public class HelloLogger {
    private static final Logger log = LoggerFactory.getLogger(HelloLogger.class);

    @GET
    @Produces(TEXT_PLAIN)
    public String showLog() {
        log.info("Here is the Log");
        return "Log has been shown";
    }
}
