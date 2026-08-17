package com.myproject.website;

import com.myproject.website.config.DotEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WebsiteApplication {

    public static void main(String[] args) {
        DotEnvLoader.load();
        SpringApplication.run(WebsiteApplication.class, args);
    }
}
