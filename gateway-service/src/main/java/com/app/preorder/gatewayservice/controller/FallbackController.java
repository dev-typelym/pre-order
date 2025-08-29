package com.app.preorder.gatewayservice.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallbackController {

    @RequestMapping(value = "/__fallback", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fallback() {
        return ResponseEntity
                .status(503)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"일시적인 장애입니다. 잠시 후 다시 시도해 주세요.\"}");
    }
}
