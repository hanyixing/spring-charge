package com.example.demo.controller;

import com.example.demo.model.ChargingRequest;
import com.example.demo.model.ChargingResult;
import com.example.demo.model.ElectricityPriceConfig;
import com.example.demo.service.ChargingOptimizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/charging")
public class ChargingController {

    @Autowired
    private ChargingOptimizationService optimizationService;

    @PostMapping("/optimize")
    public Map<String, Object> optimizeCharging(@RequestBody ChargingRequest request) {
        log.info("收到充电优化请求: {}", request);
        Map<String, Object> response = new HashMap<>();
        
        try {
            ChargingResult result = optimizationService.optimizeCharging(request);
            response.put("success", true);
            response.put("data", result);
            response.put("message", result.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("参数错误: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
        } catch (Exception e) {
            log.error("优化失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "优化失败: " + e.getMessage());
        }
        
        return response;
    }

    @GetMapping("/prices")
    public Map<String, Object> getPrices() {
        Map<String, Object> response = new HashMap<>();
        
        BigDecimal[] prices = optimizationService.get96PointPrices();
        String[] types = optimizationService.get96PointPriceTypes();
        String[] timeSlots = new String[96];
        
        for (int i = 0; i < 96; i++) {
            timeSlots[i] = optimizationService.getTimeSlotByPoint(i);
        }
        
        response.put("success", true);
        response.put("prices", prices);
        response.put("priceTypes", types);
        response.put("timeSlots", timeSlots);
        response.put("priceConfig", optimizationService.getPriceConfig());
        
        return response;
    }

    @PostMapping("/config/prices")
    public Map<String, Object> setPriceConfig(@RequestBody ElectricityPriceConfig config) {
        log.info("设置电价配置: {}", config);
        Map<String, Object> response = new HashMap<>();
        
        try {
            optimizationService.setPriceConfig(config);
            response.put("success", true);
            response.put("message", "电价配置更新成功");
            response.put("data", config);
        } catch (Exception e) {
            log.error("设置电价失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "设置电价失败: " + e.getMessage());
        }
        
        return response;
    }

    @GetMapping("/config/prices")
    public Map<String, Object> getPriceConfig() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", optimizationService.getPriceConfig());
        return response;
    }

    @PostMapping("/test")
    public Map<String, Object> testCharging() {
        Map<String, Object> response = new HashMap<>();
        
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(0);
        request.setEndPoint(95);
        request.setCurrentSoc(new BigDecimal("20"));
        request.setTargetSoc(new BigDecimal("80"));
        request.setBatteryCapacity(new BigDecimal("60"));
        request.setMaxChargingPower(new BigDecimal("7"));
        
        log.info("执行测试充电优化: {}", request);
        
        try {
            ChargingResult result = optimizationService.optimizeCharging(request);
            response.put("success", true);
            response.put("data", result);
            response.put("message", result.getMessage());
            response.put("testRequest", request);
        } catch (Exception e) {
            log.error("测试失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "测试失败: " + e.getMessage());
        }
        
        return response;
    }
}
