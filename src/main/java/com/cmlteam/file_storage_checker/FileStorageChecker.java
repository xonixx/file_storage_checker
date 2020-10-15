package com.cmlteam.file_storage_checker;

import lombok.AllArgsConstructor;

import javax.annotation.PostConstruct;

@AllArgsConstructor
public class FileStorageChecker {
  private final Req req;

  @PostConstruct
  void run() {
    Resp resp = req.get("http://localhost:8080/file");
    System.out.println(resp);
  }
}
