package com.cmlteam.file_storage_checker;

import com.cmlteam.file_storage_checker.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
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
  private int checksCount;

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
    Resp resp =
        new Req()
            .post(
                "http://localhost:9200/"
                    + esIndexName
                    + "/_delete_by_query?refresh=true&wait_for_completion=true",
                json().add("query", json().add("match_all", json())));

    if (resp.getStatus() != 200) {
      throw new RuntimeException("" + resp);
    }
  }

  private void runSuit() {
    checkCountAfterStart();
    checkCorrectFilesAddition();
    checkIncorrectFilesAddition();

    checkFileAdditionUndocumentedFields();

    checkTagsAddition();
    checkTagsAdditionNonExistentFile();
    checkTagsDuplication();
    checkTagsSearchByAnd();

    checkNonExistentFileTagDeletion();
    checkNonExistentTagDeletion();
    // TODO check existent tag deletion

    AutoTagsSupport autoTagsSupport = checkSupportsAutoTags();

    if (autoTagsSupport.supports) {
      checkFileToAutoAssignTag("aaa.mp3", autoTagsSupport.mp3Tag);
      checkFileToAutoAssignTag("aaa.bbb.ccc.mp3", autoTagsSupport.mp3Tag);
      checkFileToAutoAssignTag("AAA.MP3", autoTagsSupport.mp3Tag);
      checkFileToAutoAssignTag("звук.mp3", autoTagsSupport.mp3Tag);
      checkFileToAutoAssignTag("mp3", null);
      checkFileToAutoAssignTag("aaa.mp3.bbb", null);

      // TODO check removal of auto-assigned tag - is it possible?
    }

    // TODO check paging

    reportCollectedErrors();
  }

  private void checkNonExistentFileTagDeletion() {
    checksCount++;

    Resp resp = req.delete(endpoint + "/Non-existent-file-id/tags", jsonList().add("tag"));
    int status = resp.getStatus();
    if (status != 400 && status != 404) {
      errors.addError("Deletion tag for non-existent file should cause status 400 or 404", resp);
    }
  }

  private void checkNonExistentTagDeletion() {
    checksCount++;

    FileObj file = createFileAfterDbClean("file", 123); // no tags

    Resp resp = req.delete(endpoint + "/" + file.id + "/tags", jsonList().add("tag"));
    int status = resp.getStatus();
    if (status != 400 && status != 404) {
      errors.addError("Deletion non-existent tag should cause status 400 or 404", resp);
    }
  }

  private void checkFileAdditionUndocumentedFields() {
    checksCount++;

    String ID = "111111";
    FileObj file =
        createFileAfterDbClean(json().add("name", "file").add("size", 123).add("id", ID));

    if (ID.equals(file.id)) {
      errors.addError(
          "File creation endpoint should not use passed 'id' thus allowing to rewrite each file",
          file.resp);
    }
  }

  @Data
  static class AutoTagsSupport {
    final boolean supports;
    final String mp3Tag;
  }

  @Data
  static class FileObj {
    final String id;
    final String name;
    final Integer size;
    final List<String> tags;
    final Resp resp;
  }

  private FileObj createFileAfterDbClean(String fileName, Integer size) {
    return createFileAfterDbClean(json().add("name", fileName).add("size", size));
  }

  private FileObj createFileAfterDbClean(JsonUtil.JsonBuilder json) {
    deleteAllFilesFromES();

    checkSuccess("add file", req.post(endpoint, json));

    Resp resp = req.get(endpoint);

    Map file = (Map) ((List) resp.getJson().get("page")).get(0);
    String id = (String) file.get("id");
    if (id == null) {
      id = (String) file.get("ID");
    }
    Object tags = file.get("tags");

    return new FileObj(
        id,
        (String) file.get("name"),
        (Integer) file.get("size"),
        tags instanceof List ? (List) tags : null,
        resp);
  }

  private AutoTagsSupport checkSupportsAutoTags() {
    FileObj fileObj = createFileAfterDbClean("file.mp3", 123);
    List<String> tags = fileObj.tags;

    return tags != null && !tags.isEmpty()
        ? new AutoTagsSupport(true, tags.get(0))
        : new AutoTagsSupport(false, null);
  }

  private void checkFileToAutoAssignTag(String fileName, String shouldHaveTag) {
    checksCount++;

    FileObj fileObj = createFileAfterDbClean(fileName, 123);
    List<String> tags = fileObj.tags;

    if (shouldHaveTag != null && !List.of(shouldHaveTag).equals(tags)) {
      errors.addError(
          "File with name '" + fileName + "' should have tag '" + shouldHaveTag + "' auto-assigned",
          fileObj.resp);
    } else if (shouldHaveTag == null && tags != null && !tags.isEmpty()) {
      errors.addError(
          "File with name '" + fileName + "' should NOT have any tags auto-assigned", fileObj.resp);
    }
  }

  private void checkTagsAddition() {
    deleteAllFilesFromES();

    Resp resp =
        checkSuccess(
            "add file", req.post(endpoint, json().add("name", "file.ext1").add("size", 123)));
    String id = getId(resp);
    if (id != null) {
      checksCount++;
      List<String> tagsList = List.of("tag1", "tag2");
      addTagsCheckingSuccess(id, tagsList);
      Resp resp1 = checkSuccess("list files after tags addition", req.get(endpoint));
      List<String> tags = getTags(resp1);
      if (!checkListsEqualSorted(tagsList, tags)) {
        errors.addError("Incorrect tags returned after addition of " + tagsList, resp1);
      }

      // repeat
      checksCount++;
      addTagsCheckingSuccess(id, tagsList);
      Resp resp2 = checkSuccess("list files after tags addition", req.get(endpoint));
      List<String> tags2 = getTags(resp2);
      if (!checkListsEqualSorted(tagsList, tags2)) {
        errors.addError("Tags should not duplicate on repeating addition of " + tagsList, resp2);
      }

      // check append
      checksCount++;
      List<String> tagsList34 = List.of("tag3", "tag4");
      addTagsCheckingSuccess(id, tagsList34);
      Resp resp3 = checkSuccess("list files after tags addition", req.get(endpoint));
      List<String> tags3 = getTags(resp3);
      List<String> correct = new ArrayList<>();
      correct.addAll(tagsList);
      correct.addAll(tagsList34);

      if (!checkListsEqualSorted(correct, tags3)) {
        errors.addError(
            "Tags should be appended to existing, not replace them. Should be " + correct, resp3);
      }
    }
  }

  static boolean checkListsEqualSorted(List<String> l1, List<String> l2) {
    List<String> al1 = new ArrayList<>(l1);
    List<String> al2 = new ArrayList<>(l2);
    al1.sort(String::compareTo);
    al2.sort(String::compareTo);
    return al1.equals(al2);
  }

  private void checkTagsAdditionNonExistentFile() {
    checksCount++;

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
    checksCount++;

    deleteAllFilesFromES();

    Resp resp =
        checkSuccess(
            "add file", req.post(endpoint, json().add("name", "file.ext1").add("size", 123)));
    String id = getId(resp);
    if (id != null) {
      List<String> tagsList = List.of("tag", "tag");
      addTagsCheckingSuccess(id, tagsList);
      Resp resp1 = checkSuccess("list files after tags addition", req.get(endpoint));
      List<String> tags = getTags(resp1);
      if (checkListsEqualSorted(tagsList, tags)) {
        errors.addError("Tags should not duplicate after addition " + tagsList, resp1);
      }
    }
  }

  private void checkTagsSearchByAnd() {
    deleteAllFilesFromES();

    Resp resp1 =
        checkSuccess(
            "add file 1", req.post(endpoint, json().add("name", "file1.ext1").add("size", 123)));
    Resp resp2 =
        checkSuccess(
            "add file 2", req.post(endpoint, json().add("name", "file2.ext1").add("size", 123)));
    String id1 = getId(resp1);
    String id2 = getId(resp2);
    if (id1 != null) {
      List<String> twoTags = List.of("aaa", "bbb");
      addTagsCheckingSuccess(id1, twoTags);
      addTagsCheckingSuccess(id2, List.of("aaa"));
      String tag = "zzz";
      Resp respAll = checkSuccess("list files after tags addition", req.get(endpoint));

      checksCount++;
      if (getTotal(respAll) != 2) {
        errors.addError("Should return all records (2) ", respAll);
      }
      Resp respUnknownTag =
          checkSuccess("list files after tags addition", req.get(endpoint + "?tags=" + tag));

      checksCount++;
      if (getTotal(respUnknownTag) != 0) {
        errors.addError("Should return empty result for non-existent tag " + tag, respUnknownTag);
      }
      Resp respAnd =
          checkSuccess(
              "list files after tags addition",
              req.get(endpoint + "?tags=" + String.join(",", twoTags)));
      int totalAnd = getTotal(respAnd);

      checksCount++;
      if (totalAnd != 1) {
        errors.addError(
            "Should apply AND logic when searching by tags. In your case it's "
                + (totalAnd == 2 ? "OR" : "smth other"),
            respAnd);
      }
    }
  }

  private List<String> getTags(Resp resp) {
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
    System.out.println("TOTAL CHECKS: " + checksCount);
    System.out.println(errors.report());
    System.out.println();
  }

  private void checkCorrectFilesAddition() {
    checkCorrectlyAddedFile("zzzz.ext1", 123);
    checkCorrectlyAddedFile("ZZZZ.ext1", 123);
    checkCorrectlyAddedFile("тЕсТ.ext1", 123);
    checkCorrectlyAddedFile("test", 123);
    checkCorrectlyAddedFile("test.ext1", 0);
  }

  private void checkIncorrectFilesAddition() {
    checkIncorrectlyAddedFile("aaa.ext1", null);
    checkIncorrectlyAddedFile("aaa.ext1", -123);
    checkIncorrectlyAddedFile("", 123);
    checkIncorrectlyAddedFile(null, 123);
    checkIncorrectlyAddedFile(null, null);
  }

  private void checkIncorrectlyAddedFile(String fileName, Integer fileSize) {
    checksCount++;

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
    checksCount++;

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
    checksCount++;

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
