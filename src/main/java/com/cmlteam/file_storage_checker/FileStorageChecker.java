package com.cmlteam.file_storage_checker;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class FileStorageChecker {
  @PostConstruct
  void run() {
    System.out.println(123);
  }
}
