package com.example.demo.controller;

import com.example.demo.model.ChargeRequest;
import com.example.demo.model.ChargeResult;
import com.example.demo.model.ElectricityPrice;
import com.example.demo.service.ChargeOptimizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/charge")
public class ChargeController {

    @Autowired
    private ChargeOptimizationService optimizationService;

    @PostMapping("/optimize")
    public ChargeResult optimizeCharge(@RequestBody ChargeRequest request) {
        ElectricityPrice defaultPrice = optimizationService.createDefaultPrice();
        return optimizationService.calculateOptimalCharge(request, defaultPrice);
    }

    @PostMapping("/optimize/custom-price")
    public ChargeResult optimizeChargeWithCustomPrice(
            @RequestBody ChargeRequest request,
            @RequestParam Map<String, String> priceParams) {
        ElectricityPrice price = new ElectricityPrice();
        for (int i = 0; i < 96; i++) {
            String priceValue = priceParams.get("price" + i);
            if (priceValue != null) {
                price.setPrice(i, new BigDecimal(priceValue));
            }
        }
        return optimizationService.calculateOptimalCharge(request, price);
    }

    @GetMapping("/test")
    public Map<String, Object> testOptimization() {
        ElectricityPrice price = optimizationService.createDefaultPrice();
        ChargeRequest request = new ChargeRequest(30, 80, 0, 47);

        ChargeResult result = optimizationService.calculateOptimalCharge(request, price);

        Map<String, Object> response = new HashMap<>();
        response.put("request", request);
        response.put("result", result);
        response.put("requiredEnergyKWh", request.getRequiredEnergy());

        Map<Integer, Double> nonZeroPowers = new HashMap<>();
        for (int i = 0; i < 96; i++) {
            if (result.getChargePower(i) > 0) {
                nonZeroPowers.put(i, result.getChargePower(i));
            }
        }
        response.put("chargingTimePoints", nonZeroPowers);

        return response;
    }

    @GetMapping("/price/default")
    public ElectricityPrice getDefaultPrice() {
        return optimizationService.createDefaultPrice();
    }
}
