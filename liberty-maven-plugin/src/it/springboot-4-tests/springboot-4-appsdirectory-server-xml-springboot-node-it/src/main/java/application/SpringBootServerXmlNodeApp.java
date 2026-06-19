package application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class SpringBootServerXmlNodeApp {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootServerXmlNodeApp.class, args);
    }

    @GetMapping("/health")
    public String health() {
        return "Spring Boot 4.0 with server.xml springBoot node configuration is running!";
    }
}
