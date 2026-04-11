package com.example.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        log.info("收到请求: /api/hello");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello, Spring Boot!");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "success");
        
        return response;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "spring-boot-demo");
        response.put("timestamp", LocalDateTime.now().toString());
        
        return response;
    }

}
