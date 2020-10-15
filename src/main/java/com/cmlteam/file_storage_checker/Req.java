package com.cmlteam.file_storage_checker;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
public class Req {

    private final RestTemplate restTemplate;

    Resp get(String url, Object... uriVariables) {
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class, uriVariables);
        return new Resp(responseEntity);
    }
}
