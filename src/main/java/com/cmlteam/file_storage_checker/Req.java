package com.cmlteam.file_storage_checker;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class Req {

  private final RestTemplate restTemplate;

  private final List<ErrHandler> errorHandlers = new ArrayList<>();

  public Req() {
    this(new RestTemplate());
  }

  @FunctionalInterface
  interface ErrHandler {
    void handleError(HttpMethod httpMethod, String url, Resp resp, String errMsg);
  }

  void addErrHandler(ErrHandler errHandler) {
    errorHandlers.add(errHandler);
  }

  Resp get(String url, Object... uriVariables) {
    return exec(HttpMethod.GET, url, RequestEntity.get(URI.create(url)).build(), uriVariables);
  }

  Resp post(String url, Object body, Object... uriVariables) {
    return exec(
        HttpMethod.POST,
        url,
        RequestEntity.post(URI.create(url))
            .contentType(MediaType.APPLICATION_JSON)
            .body(body.toString()),
        uriVariables);
  }

  private Resp exec(
      HttpMethod method, String url, RequestEntity<?> requestEntity, Object... uriVariables) {
    ResponseEntity<String> responseEntity;
    try {
      responseEntity =
          restTemplate.exchange(url, method, requestEntity, String.class, uriVariables);
    } catch (RestClientResponseException e) {
      responseEntity =
          ResponseEntity.status(e.getRawStatusCode())
              .headers(e.getResponseHeaders())
              .body(e.getResponseBodyAsString());
    }
    Resp resp = new Resp(requestEntity, responseEntity);
    for (ErrHandler errorHandler : errorHandlers) {
      for (String error : resp.getErrors()) {
        errorHandler.handleError(method, url, resp, error);
      }
    }
    return resp;
  }
}
