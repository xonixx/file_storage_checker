package com.cmlteam.file_storage_checker;

import com.cmlteam.file_storage_checker.util.JsonUtil;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public class Resp {
  private final ResponseEntity<String> responseEntity;
  private final Map<String, ?> json;

  public Resp(ResponseEntity<String> responseEntity) {
    this.responseEntity = responseEntity;
    json = JsonUtil.parseJson(responseEntity.getBody());
  }

  public int getStatus() {
    return responseEntity.getStatusCodeValue();
  }

  @Override
  public String toString() {
    return responseEntity.toString();
  }
}
