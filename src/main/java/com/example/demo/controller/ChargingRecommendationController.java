package com.example.demo.controller;

import com.example.demo.dto.ChargingRecommendationRequest;
import com.example.demo.dto.ChargingRecommendationResponse;
import com.example.demo.service.ChargingRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@RestController
@RequestMapping("/api/charging")
@RequiredArgsConstructor
public class ChargingRecommendationController {

    private final ChargingRecommendationService recommendationService;

    @PostMapping("/recommend")
    public ChargingRecommendationResponse recommend(@RequestBody ChargingRecommendationRequest request) {
        log.info("收到充电推荐请求, 用户ID: {}", request.getUserId());
        
        long startTime = System.currentTimeMillis();
        
        List<Double> historicalCurve = request.getHistoricalCurve();
        List<Double> priceCurve = request.getPriceCurve();
        List<Double> gridLoadCurve = request.getGridLoadCurve();
        
        if (historicalCurve == null || historicalCurve.isEmpty()) {
            historicalCurve = generateDefaultHistoricalCurve();
        }
        if (priceCurve == null || priceCurve.isEmpty()) {
            priceCurve = generateDefaultPriceCurve();
        }
        if (gridLoadCurve == null || gridLoadCurve.isEmpty()) {
            gridLoadCurve = generateDefaultGridLoadCurve();
        }
        
        double totalDemand = request.getTotalDemand() > 0 ? request.getTotalDemand() : 50.0;
        double maxPower = request.getMaxPower() > 0 ? request.getMaxPower() : 7.0;
        double minPower = request.getMinPower() >= 0 ? request.getMinPower() : 0.0;
        
        double currentSoc = request.getCurrentSoc() >= 0 ? request.getCurrentSoc() : 20.0;
        double targetSoc = request.getTargetSoc() > 0 ? request.getTargetSoc() : 80.0;
        double batteryCapacity = request.getBatteryCapacity() > 0 ? request.getBatteryCapacity() : 60.0;
        double socMin = request.getSocMin() >= 0 ? request.getSocMin() : 10.0;
        double socMax = request.getSocMax() > 0 ? request.getSocMax() : 95.0;
        int availableTimeSlots = request.getAvailableTimeSlots() > 0 ? request.getAvailableTimeSlots() : 96;
        
        List<Double> recommendedCurve = recommendationService.recommendWithSoc(
                historicalCurve, priceCurve, gridLoadCurve,
                totalDemand, maxPower, minPower,
                currentSoc, targetSoc, batteryCapacity, socMin, socMax, availableTimeSlots);
        
        double totalCost = recommendationService.calculateTotalCost(recommendedCurve, priceCurve);
        double peakValleyScore = recommendationService.calculatePeakValleyScore(recommendedCurve, gridLoadCurve);
        double habitMatchScore = recommendationService.calculateHabitMatchScore(recommendedCurve, historicalCurve);
        double totalEnergy = recommendedCurve.stream().mapToDouble(Double::doubleValue).sum();
        
        List<Double> socCurve = recommendationService.calculateSocCurve(recommendedCurve, currentSoc, batteryCapacity);
        double finalSoc = socCurve.get(socCurve.size() - 1);
        
        ChargingRecommendationResponse response = new ChargingRecommendationResponse();
        response.setUserId(request.getUserId());
        response.setRecommendedCurve(recommendedCurve);
        response.setTotalCost(totalCost);
        response.setPeakValleyScore(peakValleyScore);
        response.setHabitMatchScore(habitMatchScore);
        response.setTotalEnergy(totalEnergy);
        response.setAlgorithm("Multi-Objective-Optimization-With-SOC");
        response.setComputationTimeMs(System.currentTimeMillis() - startTime);
        response.setInitialSoc(currentSoc);
        response.setFinalSoc(finalSoc);
        response.setSocCurve(socCurve);
        response.setSocMinConstraint(socMin);
        response.setSocMaxConstraint(socMax);
        
        log.info("充电推荐完成, 总成本: {}, 削峰平谷得分: {}, 习惯匹配得分: {}, SOC: {}% -> {}%", 
                totalCost, peakValleyScore, habitMatchScore, currentSoc, finalSoc);
        
        return response;
    }

    @GetMapping("/demo")
    public ChargingRecommendationResponse demo() {
        log.info("运行演示案例...");
        
        ChargingRecommendationRequest request = new ChargingRecommendationRequest();
        request.setUserId("demo-user-001");
        request.setHistoricalCurve(generateDefaultHistoricalCurve());
        request.setPriceCurve(generateDefaultPriceCurve());
        request.setGridLoadCurve(generateDefaultGridLoadCurve());
        request.setTotalDemand(50.0);
        request.setMaxPower(7.0);
        request.setMinPower(0.0);
        request.setCurrentSoc(20.0);
        request.setTargetSoc(80.0);
        request.setBatteryCapacity(60.0);
        request.setSocMin(10.0);
        request.setSocMax(95.0);
        request.setAvailableTimeSlots(96);
        
        return recommend(request);
    }
    
    @GetMapping("/demo-soc")
    public ChargingRecommendationResponse demoWithSoc(
            @RequestParam(defaultValue = "30") double currentSoc,
            @RequestParam(defaultValue = "90") double targetSoc,
            @RequestParam(defaultValue = "60") double batteryCapacity) {
        
        log.info("运行SOC演示案例, SOC: {}% -> {}%, 电池容量: {} kWh", currentSoc, targetSoc, batteryCapacity);
        
        ChargingRecommendationRequest request = new ChargingRecommendationRequest();
        request.setUserId("demo-soc-user");
        request.setHistoricalCurve(generateDefaultHistoricalCurve());
        request.setPriceCurve(generateDefaultPriceCurve());
        request.setGridLoadCurve(generateDefaultGridLoadCurve());
        request.setMaxPower(7.0);
        request.setMinPower(0.0);
        request.setCurrentSoc(currentSoc);
        request.setTargetSoc(targetSoc);
        request.setBatteryCapacity(batteryCapacity);
        request.setSocMin(10.0);
        request.setSocMax(95.0);
        request.setAvailableTimeSlots(96);
        
        return recommend(request);
    }

    private List<Double> generateDefaultHistoricalCurve() {
        List<Double> curve = new ArrayList<>();
        Random random = new Random(42);
        
        for (int i = 0; i < 96; i++) {
            int hour = i / 4;
            double baseValue;
            
            if (hour >= 22 || hour < 6) {
                baseValue = 5.0 + random.nextDouble() * 2;
            } else if (hour >= 18 && hour < 22) {
                baseValue = 3.0 + random.nextDouble() * 2;
            } else if (hour >= 12 && hour < 14) {
                baseValue = 2.0 + random.nextDouble();
            } else {
                baseValue = 0.5 + random.nextDouble() * 0.5;
            }
            
            curve.add(baseValue);
        }
        
        return curve;
    }

    private List<Double> generateDefaultPriceCurve() {
        List<Double> curve = new ArrayList<>();
        
        for (int i = 0; i < 96; i++) {
            int hour = i / 4;
            double price;
            
            if (hour >= 10 && hour < 15) {
                price = 1.2;
            } else if (hour >= 18 && hour < 21) {
                price = 1.5;
            } else if (hour >= 23 || hour < 7) {
                price = 0.4;
            } else if (hour >= 7 && hour < 10) {
                price = 0.8;
            } else {
                price = 1.0;
            }
            
            curve.add(price);
        }
        
        return curve;
    }

    private List<Double> generateDefaultGridLoadCurve() {
        List<Double> curve = new ArrayList<>();
        
        for (int i = 0; i < 96; i++) {
            int hour = i / 4;
            double load;
            
            if (hour >= 9 && hour < 12) {
                load = 0.85 + 0.1 * Math.sin(hour * Math.PI / 6);
            } else if (hour >= 14 && hour < 17) {
                load = 0.9 + 0.05 * Math.sin(hour * Math.PI / 6);
            } else if (hour >= 18 && hour < 21) {
                load = 0.95 + 0.05 * Math.sin(hour * Math.PI / 6);
            } else if (hour >= 23 || hour < 6) {
                load = 0.4 + 0.1 * Math.sin(hour * Math.PI / 6);
            } else {
                load = 0.6 + 0.15 * Math.sin(hour * Math.PI / 6);
            }
            
            curve.add(Math.max(0, Math.min(1, load)));
        }
        
        return curve;
    }
}
