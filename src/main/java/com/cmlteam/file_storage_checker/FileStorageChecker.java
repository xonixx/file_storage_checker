package com.cmlteam.file_storage_checker;

import com.cmlteam.file_storage_checker.util.JsonUtil;
import lombok.AllArgsConstructor;

import javax.annotation.PostConstruct;

@AllArgsConstructor
public class FileStorageChecker {
  private final Req req;

  @PostConstruct
  void run() {
    String ENDPOINT = "http://localhost:8080/file";
    Resp resp = req.get(ENDPOINT);
    System.out.println(resp);

    Resp resp1 = req.post(ENDPOINT, JsonUtil.json().add("name", "ZZZZ.txt").add("size", 123));

    System.out.println(resp1);

    Resp resp3 = req.get(ENDPOINT);
    System.out.println(resp3);
  }
}
