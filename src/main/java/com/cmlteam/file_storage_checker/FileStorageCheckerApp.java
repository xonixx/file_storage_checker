package com.cmlteam.file_storage_checker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@ComponentScan(basePackages = "com.cmlteam")
public class FileStorageCheckerApp {

  @Bean
  RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean
  Req req(RestTemplate restTemplate) {
    return new Req(restTemplate);
  }

  @Bean
  FileStorageCheckerCli fileStorageCheckerCli(Req req) {
    return new FileStorageCheckerCli(req);
  }

  public static void main(String[] args) {
    SpringApplication.run(FileStorageCheckerApp.class, args);
  }
}
