package com.cmlteam.file_storage_checker;

import com.cmlteam.file_storage_checker.util.JsonUtil;
import lombok.AllArgsConstructor;

import javax.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;

import static com.cmlteam.file_storage_checker.util.JsonUtil.json;

@AllArgsConstructor
public class FileStorageChecker {
  private final Req req;
  private final String endpoint = "http://localhost:8080/file";
  private final Errors errors = new Errors();

  @PostConstruct
  void run() {
    setup();

    try {
      runSuit();
    } finally {
      deleteAllFilesFromES();
    }
  }

  private void setup() {
    req.addErrHandler(
        (httpMethod, url, resp, errMsg) ->
            errors.addError(httpMethod.name() + " " + url + " : " + errMsg, resp));
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
    checkCountAfterStart();
    checkCorrectFilesAddition();
    checkIncorrectFilesAddition();

    reportCollectedErrors();
  }

  private void reportCollectedErrors() {
    System.out.println();
    System.out.println(errors.report());
    System.out.println();
  }

  private void checkCorrectFilesAddition() {
    checkCorrectlyAddedFile("zzzz.txt", 123);
    checkCorrectlyAddedFile("ZZZZ.txt", 123);
    checkCorrectlyAddedFile("тЕсТ.txt", 123);
    checkCorrectlyAddedFile("test", 123);
    checkCorrectlyAddedFile("test.txt", 0);
  }

  private void checkIncorrectFilesAddition() {
    checkIncorrectlyAddedFile("aaa.txt", null);
    checkIncorrectlyAddedFile("aaa.txt", -123);
    checkIncorrectlyAddedFile(null, 123);
    checkIncorrectlyAddedFile(null, null);
  }

  private void checkIncorrectlyAddedFile(String fileName, Integer fileSize) {
    JsonUtil.JsonBuilder file = json();
    if (fileName != null) file.add("name", fileName);
    if (fileSize != null) file.add("size", fileSize);

    Resp resp = req.post(endpoint, file);

    List<String> err = new ArrayList<>();

    int status = resp.getStatus();

    if (status != 400) {
      err.add(
          "File '"
              + fileName
              + "' of size "
              + fileSize
              + " should cause status 400 but resulted in status="
              + status);
      err.add("" + resp.getJson());
    }

    if (!err.isEmpty()) {
      errors.addError(String.join(", ", err), resp);
    }
  }

  private void checkCorrectlyAddedFile(String fileName, int fileSize) {
    Resp resp = req.post(endpoint, json().add("name", fileName).add("size", fileSize));

    List<String> err = new ArrayList<>();

    int status = resp.getStatus();

    if (status != 200 && status != 201) {
      err.add(
          "File '"
              + fileName
              + "' of size "
              + fileSize
              + " should save correctly but resulted in status="
              + status);
      err.add("" + resp.getJson());
    }

    if (!err.isEmpty()) {
      errors.addError(String.join(", ", err), resp);
    }
  }

  private void checkCountAfterStart() {
    Resp resp = req.get(endpoint);

    int total = (Integer) resp.getJson().get("total");

    if (total > 0) {
      errors.addError("The storage contains some files just after start - should be empty", resp);
    }
  }
}
