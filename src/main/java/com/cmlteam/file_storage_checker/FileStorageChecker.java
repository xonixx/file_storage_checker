package com.cmlteam.file_storage_checker;

import com.cmlteam.file_storage_checker.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.cmlteam.file_storage_checker.util.JsonUtil.json;
import static com.cmlteam.file_storage_checker.util.JsonUtil.jsonList;

@AllArgsConstructor
public class FileStorageChecker {
  private final Errors errors = new Errors();
  private final Req req;
  private final String endpoint;
  private final String esIndexName;
  private final boolean tagsAsFile;

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

  @SneakyThrows
  private void deleteAllFilesFromES() {
    Thread.sleep(1000); // TODO
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

    checkTagsAddition();
    checkTagsAdditionNonExistentFile();
    checkTagsDuplication();
    checkTagsSearchByAnd();

    reportCollectedErrors();
  }

  private void checkTagsAddition() {
    deleteAllFilesFromES();

    Resp resp =
        checkSuccess(
            "add file", req.post(endpoint, json().add("name", "file.xyz").add("size", 123)));
    String id = getId(resp);
    if (id != null) {
      List<String> tagsList = List.of("tag1", "tag2");
      addTagsCheckingSuccess(id, tagsList);
      Resp resp1 = checkSuccess("list files after tags addition", req.get(endpoint));
      List tags = getTags(resp1);
      if (!tagsList.equals(tags)) {
        errors.addError("Incorrect tags returned after addition of " + tagsList, resp1);
      }

      // repeat
      addTagsCheckingSuccess(id, tagsList);
      Resp resp2 = checkSuccess("list files after tags addition", req.get(endpoint));
      List tags2 = getTags(resp2);
      if (!tagsList.equals(tags2)) {
        errors.addError("Tags should not duplicate on repeating addition of " + tagsList, resp2);
      }

      // check append
      List<String> tagsList34 = List.of("tag3", "tag4");
      addTagsCheckingSuccess(id, tagsList34);
      Resp resp3 = checkSuccess("list files after tags addition", req.get(endpoint));
      List tags3 = getTags(resp3);
      List<String> correct = new ArrayList<>();
      correct.addAll(tagsList);
      correct.addAll(tagsList34);
      if (!correct.equals(tags3)) {
        errors.addError(
            "Tags should be appended to existing, not replace them. Should be " + correct, resp3);
      }
    }
  }

  private void checkTagsAdditionNonExistentFile() {
    deleteAllFilesFromES();

    List<String> tagsList = List.of("tag1", "tag2");
    Resp resp = addTags("non-existent-file-1231233", tagsList);
    int status = resp.getStatus();
    if (status != 404) {
      errors.addError(
          "Should not return status " + status + " when adding tags to non-existent file", resp);
    }
  }

  private void checkTagsDuplication() {
    deleteAllFilesFromES();

    Resp resp =
        checkSuccess(
            "add file", req.post(endpoint, json().add("name", "file.xyz").add("size", 123)));
    String id = getId(resp);
    if (id != null) {
      List<String> tagsList = List.of("tag", "tag");
      addTagsCheckingSuccess(id, tagsList);
      Resp resp1 = checkSuccess("list files after tags addition", req.get(endpoint));
      List tags = getTags(resp1);
      if (tagsList.equals(tags)) {
        errors.addError("Tags should not duplicate after addition " + tagsList, resp1);
      }
    }
  }

  private void checkTagsSearchByAnd() {
    deleteAllFilesFromES();

    Resp resp1 =
        checkSuccess(
            "add file 1", req.post(endpoint, json().add("name", "file1.xyz").add("size", 123)));
    Resp resp2 =
        checkSuccess(
            "add file 2", req.post(endpoint, json().add("name", "file2.xyz").add("size", 123)));
    String id1 = getId(resp1);
    String id2 = getId(resp2);
    if (id1 != null) {
      List<String> twoTags = List.of("aaa", "bbb");
      addTagsCheckingSuccess(id1, twoTags);
      addTagsCheckingSuccess(id2, List.of("aaa"));
      String tag = "zzz";
      Resp respAll = checkSuccess("list files after tags addition", req.get(endpoint));
      if (getTotal(respAll) != 2) {
        errors.addError("Should return all records (2) ", respAll);
      }
      Resp respUnknownTag =
          checkSuccess("list files after tags addition", req.get(endpoint + "?tags=" + tag));
      if (getTotal(respUnknownTag) != 0) {
        errors.addError("Should return empty result for non-existent tag " + tag, respUnknownTag);
      }
      Resp respAnd =
          checkSuccess(
              "list files after tags addition",
              req.get(endpoint + "?tags=" + String.join(",", twoTags)));
      int totalAnd = getTotal(respAnd);
      if (totalAnd != 1) {
        errors.addError(
            "Should apply AND logic when searching by tags. In your case it's "
                + (totalAnd == 2 ? "OR" : "smth other"),
            respAnd);
      }
    }
  }

  private List getTags(Resp resp) {
    Map<String, ?> json = resp.getJson();

    List page = (List) json.get("page");
    Map file = (Map) page.get(0);
    return (List) file.get("tags");
  }

  private int getTotal(Resp resp) {
    Map<String, ?> json = resp.getJson();

    return (int) json.get("total");
  }

  private Resp addTagsCheckingSuccess(String id, List<String> tagsList) {
    // need this to show an error
    Resp resp =
        checkSuccess("add tags", req.post(endpoint + "/" + id + "/tags", jsonList(tagsList)));
    if (tagsAsFile) {
      resp =
          checkSuccess(
              "add tags",
              req.post(endpoint + "/" + id + "/tags", json().add("tags", jsonList(tagsList))));
    }
    return resp;
  }

  private Resp addTags(String id, List<String> tagsList) {
    Resp resp;
    if (!tagsAsFile) {
      resp = req.post(endpoint + "/" + id + "/tags", jsonList(tagsList));
    } else {
      resp = req.post(endpoint + "/" + id + "/tags", json().add("tags", tagsList));
    }
    return resp;
  }

  Resp checkSuccess(String msg, Resp resp) {
    List<String> err = new ArrayList<>();

    int status = resp.getStatus();

    if (status != 200) {
      err.add(msg + " should execute correctly but resulted in status=" + status);
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
    checkCorrectlyAddedFile("zzzz.xyz", 123);
    checkCorrectlyAddedFile("ZZZZ.xyz", 123);
    checkCorrectlyAddedFile("тЕсТ.xyz", 123);
    checkCorrectlyAddedFile("test", 123);
    checkCorrectlyAddedFile("test.xyz", 0);
  }

  private void checkIncorrectFilesAddition() {
    checkIncorrectlyAddedFile("aaa.xyz", null);
    checkIncorrectlyAddedFile("aaa.xyz", -123);
    checkIncorrectlyAddedFile(null, 123);
    checkIncorrectlyAddedFile(null, null);
  }

  private void checkIncorrectlyAddedFile(String fileName, Integer fileSize) {
    JsonUtil.JsonMapBuilder file = json();
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

    if (resp.getStatus() != 200) {
      errors.addError("Error checking count", resp);
      return;
    }
    int total = (Integer) resp.getJson().get("total");

    if (total > 0) {
      errors.addError("The storage contains some files just after start - should be empty", resp);
    }
  }
}
