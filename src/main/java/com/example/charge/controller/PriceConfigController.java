package com.example.charge.controller;

import com.example.charge.model.ElectricityPrice;
import com.example.charge.model.PriceUpdateRequest;
import com.example.charge.service.PriceConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/price")
public class PriceConfigController {

    @Autowired
    private PriceConfigService priceConfigService;

    @GetMapping("/list")
    public ResponseEntity<List<ElectricityPrice>> getAllPrices() {
        return ResponseEntity.ok(priceConfigService.getAllPrices());
    }

    @GetMapping("/{timePoint}")
    public ResponseEntity<ElectricityPrice> getPriceByPoint(@PathVariable Integer timePoint) {
        ElectricityPrice price = priceConfigService.getPriceByPoint(timePoint);
        if (price == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(price);
    }

    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updatePrice(@RequestBody PriceUpdateRequest request) {
        Map<String, Object> result = new HashMap<>();

        if (request.getTimePoint() != null && request.getPrice() != null) {
            boolean success = priceConfigService.updatePrice(
                    request.getTimePoint(),
                    request.getPrice(),
                    request.getPeriodType() != null ? request.getPeriodType() : "自定义"
            );
            result.put("success", success);
            result.put("message", success ? "单个时间点电价更新成功" : "更新失败");
        } else if (request.getPrices() != null && !request.getPrices().isEmpty()) {
            boolean success = priceConfigService.updatePrices(request.getPrices());
            result.put("success", success);
            result.put("message", "批量更新电价成功，共更新 " + request.getPrices().size() + " 个时间点");
        } else {
            result.put("success", false);
            result.put("message", "参数不完整");
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/period/{periodType}")
    public ResponseEntity<Map<String, Object>> updatePriceByPeriod(
            @PathVariable String periodType,
            @RequestBody PriceUpdateRequest request) {
        Map<String, Object> result = new HashMap<>();

        if (request.getPrice() == null) {
            result.put("success", false);
            result.put("message", "请提供电价");
            return ResponseEntity.badRequest().body(result);
        }

        boolean success = priceConfigService.updatePriceByPeriod(periodType, request.getPrice());
        result.put("success", success);
        result.put("message", "已更新时段[" + periodType + "]的电价为: " + request.getPrice() + " 元/kWh");

        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetToDefault() {
        priceConfigService.resetToDefault();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "已重置为默认电价");
        return ResponseEntity.ok(result);
    }
}
