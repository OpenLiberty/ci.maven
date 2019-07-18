package com.demo;

import javax.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

@Health
@ApplicationScoped
public class SystemHealth implements HealthCheck {
  @Override
  public HealthCheckResponse call() {
    if (!System.getProperty("wlp.server.name").startsWith("defaultServer")) {
      return HealthCheckResponse.named(HelloWorld.class.getSimpleName())
                                .withData("default server", "not available").down()
                                .build();
    }
    return HealthCheckResponse.named(HelloWorld.class.getSimpleName())
                              .withData("default server", "available").up().build();
  }
}

