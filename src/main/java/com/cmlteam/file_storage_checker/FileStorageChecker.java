package com.cmlteam.file_storage_checker;

import com.cmlteam.file_storage_checker.util.JsonUtil;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.cmlteam.file_storage_checker.util.JsonUtil.json;

@AllArgsConstructor
public class FileStorageChecker {
  private final Errors errors = new Errors();
  private final Req req;
  private final String endpoint;
  private final String esIndexName;

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
                "http://localhost:9200/" + esIndexName + "/_delete_by_query",
                json().add("query", json().add("match_all", json())));

    if (resp.getStatus() != 200) {
      throw new RuntimeException("" + resp);
    }
  }

  private void runSuit() {
    checkCountAfterStart();
    checkCorrectFilesAddition();
    checkIncorrectFilesAddition();

    deleteAllFilesFromES();
    checkTagsAddition();

    reportCollectedErrors();
  }

  private void checkTagsAddition() {
    Resp resp =
        checkSuccess(
            "add file", req.post(endpoint, json().add("name", "file.txt").add("size", 123)));
    String id = getId(resp);
    if (id != null) {
      checkSuccess("add tags", req.post(endpoint + "/" + id + "/tags", List.of("tag1", "tag2")));
      checkSuccess("list files after tags addition", req.get(endpoint));
    }
  }

  Resp checkSuccess(String msg, Resp resp) {
    List<String> err = new ArrayList<>();

    int status = resp.getStatus();

    if (status != 200) {
      err.add(msg + " should execute correctly but resulted in status=" + status);
      err.add("" + resp.getJson());
    }

    if (!err.isEmpty()) {
      errors.addError(String.join(", ", err), resp);
    }
    return resp;
  }

  private String getId(Resp resp) {
    Map<String, ?> json = resp.getJson();
    Object id;
    if (json == null) {
      id = null;
    } else {
      id = json.get("id");
      if (id == null) {
        id = json.get("ID");
      }
    }
    if (!(id instanceof String)) {
      errors.addError("ID of created file is not a String: " + id, resp);
      return null;
    }
    return (String) id;
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

    // TODO check success + ID

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
