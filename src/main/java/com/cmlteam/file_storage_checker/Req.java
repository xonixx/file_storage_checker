package com.cmlteam.file_storage_checker;

import com.cmlteam.file_storage_checker.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@RequiredArgsConstructor
public class Req {

  private final RestTemplate restTemplate;

  Resp get(String url, Object... uriVariables) {
    return exec(() -> restTemplate.getForEntity(url, String.class, uriVariables));
  }

  Resp post(String url, JsonUtil.JsonBuilder body, Object... uriVariables) {
    return exec(
        () ->
            restTemplate.postForEntity(
                url,
                RequestEntity.post(URI.create(url))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString()),
                String.class,
                uriVariables));
  }

  @FunctionalInterface
  private interface Call {
    ResponseEntity<String> call();
  }

  private Resp exec(Call call) {
    ResponseEntity<String> responseEntity;
    try {
      responseEntity = call.call();
    } catch (RestClientResponseException e) {
      responseEntity =
          ResponseEntity.status(e.getRawStatusCode())
              .headers(e.getResponseHeaders())
              .body(e.getResponseBodyAsString());
    }
    return new Resp(responseEntity);
  }
}
