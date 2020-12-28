package com.cmlteam.file_storage_checker;

import com.cmlteam.file_storage_checker.util.JsonUtil;
import lombok.Getter;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Resp {
  private final RequestEntity<?> requestEntity;
  private final ResponseEntity<String> responseEntity;

  @Getter private final Map<String, ?> json; // TODO make this something more clever

  @Getter private final List<String> errors = new ArrayList<>();

  public Resp(RequestEntity<?> requestEntity, ResponseEntity<String> responseEntity) {
    this.requestEntity = requestEntity;
    this.responseEntity = responseEntity;
    Map<String, ?> json1;
    try {
      json1 = JsonUtil.parseJson(responseEntity.getBody());
    } catch (JsonUtil.JsonParseException ex) {
      errors.add("Body is not a valid JSON");
      json1 = null;
    }

    json = json1;

    MediaType contentType = responseEntity.getHeaders().getContentType();
    if (!MediaType.APPLICATION_JSON.includes(contentType)) {
      errors.add("Wrong content type for JSON: " + contentType);
    }
  }

  public int getStatus() {
    return responseEntity.getStatusCodeValue();
  }

  @Override
  public String toString() {
    return "----- REQUEST -----\n"
        + requestEntity.toString()
        + "\n----- RESPONSE -----\n"
        + responseEntity.toString()
        + "\n--------------------";
  }
}
