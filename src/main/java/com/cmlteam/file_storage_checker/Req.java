package com.cmlteam.file_storage_checker;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
public class Req {

  private final RestTemplate restTemplate;

  Resp get(String url, Object... uriVariables) {
    ResponseEntity<String> responseEntity;
    try {

      responseEntity = restTemplate.getForEntity(url, String.class, uriVariables);
    } catch (RestClientResponseException e) {
      responseEntity =
          ResponseEntity.status(e.getRawStatusCode())
              .headers(e.getResponseHeaders())
              .body(e.getResponseBodyAsString());
    }
    return new Resp(responseEntity);
  }
}
