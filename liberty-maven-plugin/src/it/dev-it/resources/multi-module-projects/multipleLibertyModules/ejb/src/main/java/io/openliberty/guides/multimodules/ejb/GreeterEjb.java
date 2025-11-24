package io.openliberty.guides.multimodules.ejb;

import javax.ejb.Stateless;

@Stateless
public class GreeterEjb {

    public String getGreeting() {
        return "Hello from EJB";
    }
}
