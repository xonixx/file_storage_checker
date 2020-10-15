package com.cmlteam.file_storage_checker;

import com.cmlteam.file_storage_checker.util.JsonUtil;
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
    return exec(
        HttpMethod.GET, url, () -> restTemplate.getForEntity(url, String.class, uriVariables));
  }

  Resp post(String url, Object body, Object... uriVariables) {
    return exec(
        HttpMethod.POST,
        url,
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

  private Resp exec(HttpMethod httpMethod, String url, Call call) {
    ResponseEntity<String> responseEntity;
    try {
      responseEntity = call.call();
    } catch (RestClientResponseException e) {
      responseEntity =
          ResponseEntity.status(e.getRawStatusCode())
              .headers(e.getResponseHeaders())
              .body(e.getResponseBodyAsString());
    }
    Resp resp = new Resp(responseEntity);
    for (ErrHandler errorHandler : errorHandlers) {
      for (String error : resp.getErrors()) {
        errorHandler.handleError(httpMethod, url, resp, error);
      }
    }
    return resp;
  }
}
