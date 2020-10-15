package com.cmlteam.file_storage_checker;

import lombok.AllArgsConstructor;

import javax.annotation.PostConstruct;

import static com.cmlteam.file_storage_checker.util.JsonUtil.json;

@AllArgsConstructor
public class FileStorageChecker {
  private final Req req;

  @PostConstruct
  void run() {
    try {
      runSuit();
    } finally {
      deleteAllFilesFromES();
    }
  }

  private void deleteAllFilesFromES() {
    Resp resp =
        new Req()
            .post(
                "http://localhost:9200/file/_delete_by_query",
                json().add("query", json().add("match_all", json())));

    if (resp.getStatus() != 200) {
      throw new RuntimeException("" + resp);
    }
  }

  private void runSuit() {
    String ENDPOINT = "http://localhost:8080/file";

    Errors errors = new Errors();

    req.addErrHandler(
        (httpMethod, url, resp, errMsg) -> {
          errors.addError(httpMethod.name() + " " + url + " : " + errMsg, resp);
        });

    Resp resp = req.get(ENDPOINT);

    int total = (Integer) resp.getJson().get("total");

    if (total > 0) {
      errors.addError("The storage contains some files just after start - should be empty", resp);
    }

    System.out.println(resp);

    Resp resp1 = req.post(ENDPOINT, json().add("name", "zzzz.txt").add("size", 123));

    System.out.println(resp1);

    Resp resp3 = req.get(ENDPOINT);
    System.out.println(resp3);

    System.out.println();
    System.out.println(errors.report());
    System.out.println();
  }
}
