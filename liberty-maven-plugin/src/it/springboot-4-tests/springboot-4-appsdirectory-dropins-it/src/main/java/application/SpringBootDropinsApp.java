package application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class SpringBootDropinsApp {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootDropinsApp.class, args);
    }

    @RequestMapping("/")
    public String hello() {
        return "HELLO SPRING BOOT 4.0!!";
    }
}
