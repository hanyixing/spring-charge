package com.example.demo.controller;

import com.example.demo.config.ElectricityPriceConfig;
import com.example.demo.dto.ChargingRequest;
import com.example.demo.dto.ChargingResponse;
import com.example.demo.dto.PriceConfigRequest;
import com.example.demo.service.ChargingOptimizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/charging")
@RequiredArgsConstructor
public class ChargingController {

    private final ChargingOptimizationService optimizationService;
    private final ElectricityPriceConfig priceConfig;

    @PostMapping("/optimize")
    public ResponseEntity<ChargingResponse> optimizeCharging(@Valid @RequestBody ChargingRequest request) {
        log.info("收到充电优化请求: {}", request);
        ChargingResponse response = optimizationService.optimizeCharging(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/price/config")
    public ResponseEntity<Map<String, Object>> getPriceConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("sharpPrice", priceConfig.getSharpPrice());
        config.put("peakPrice", priceConfig.getPeakPrice());
        config.put("flatPrice", priceConfig.getFlatPrice());
        config.put("valleyPrice", priceConfig.getValleyPrice());
        return ResponseEntity.ok(config);
    }

    @PostMapping("/price/config")
    public ResponseEntity<Map<String, Object>> updatePriceConfig(@Valid @RequestBody PriceConfigRequest request) {
        log.info("更新电价配置: {}", request);
        
        priceConfig.setSharpPrice(request.getSharpPrice());
        priceConfig.setPeakPrice(request.getPeakPrice());
        priceConfig.setFlatPrice(request.getFlatPrice());
        priceConfig.setValleyPrice(request.getValleyPrice());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "电价配置更新成功");
        response.put("sharpPrice", priceConfig.getSharpPrice());
        response.put("peakPrice", priceConfig.getPeakPrice());
        response.put("flatPrice", priceConfig.getFlatPrice());
        response.put("valleyPrice", priceConfig.getValleyPrice());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/price/schedule")
    public ResponseEntity<Map<String, Object>> getPriceSchedule() {
        Map<String, Object> schedule = new HashMap<>();
        
        for (int i = 0; i < 96; i++) {
            int hour = i / 4;
            int minute = (i % 4) * 15;
            String timeStr = String.format("%02d:%02d", hour, minute);
            
            Map<String, Object> pointInfo = new HashMap<>();
            pointInfo.put("point", i);
            pointInfo.put("time", timeStr);
            pointInfo.put("period", priceConfig.getPeriodByPoint(i));
            pointInfo.put("price", priceConfig.getPriceByPoint(i));
            
            schedule.put(String.valueOf(i), pointInfo);
        }
        
        return ResponseEntity.ok(schedule);
    }
}
