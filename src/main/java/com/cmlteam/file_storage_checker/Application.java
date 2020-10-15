package com.cmlteam.file_storage_checker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@ComponentScan(basePackages = "com.cmlteam")
public class Application {

  @Bean
  RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean
  Req req(RestTemplate restTemplate) {
    return new Req(restTemplate);
  }

  @Bean
  FileStorageChecker fileStorageChecker(Req req) {
    return new FileStorageChecker(req);
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
