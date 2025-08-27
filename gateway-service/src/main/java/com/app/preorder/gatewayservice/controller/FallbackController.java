package com.app.preorder.gatewayservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class FallbackController {
    @RequestMapping("/__fallback")
    public ResponseEntity<String> fallback() {
        return ResponseEntity.status(503).body("Temporary issue. Please retry.");
    }
}
