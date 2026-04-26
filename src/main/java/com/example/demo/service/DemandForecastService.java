package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class DemandForecastService {
    
    private final Random random = new Random(42);
    
    public List<Double> forecastDemand(List<Double> historicalData, int forecastPoints) {
        log.info("开始预测未来{}个时间点的充放电需求", forecastPoints);
        
        List<Double> forecast = new ArrayList<>();
        
        if (historicalData == null || historicalData.isEmpty()) {
            forecast = generateDefaultDemandPattern(forecastPoints);
        } else {
            forecast = forecastBasedOnHistory(historicalData, forecastPoints);
        }
        
        log.info("需求预测完成，平均需求: {}", 
                forecast.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        
        return forecast;
    }
    
    private List<Double> generateDefaultDemandPattern(int points) {
        List<Double> demand = new ArrayList<>();
        
        for (int i = 0; i < points; i++) {
            int hour = i / 4;
            double baseDemand;
            
            if (hour >= 8 && hour < 12) {
                baseDemand = 50 + random.nextDouble() * 30;
            } else if (hour >= 14 && hour < 18) {
                baseDemand = 60 + random.nextDouble() * 40;
            } else if (hour >= 18 && hour < 22) {
                baseDemand = 80 + random.nextDouble() * 50;
            } else if (hour >= 22 || hour < 6) {
                baseDemand = 20 + random.nextDouble() * 20;
            } else {
                baseDemand = 30 + random.nextDouble() * 25;
            }
            
            demand.add(Math.round(baseDemand * 100.0) / 100.0);
        }
        
        return demand;
    }
    
    private List<Double> forecastBasedOnHistory(List<Double> historicalData, int forecastPoints) {
        List<Double> forecast = new ArrayList<>();
        
        double avg = historicalData.stream().mapToDouble(Double::doubleValue).average().orElse(50.0);
        double std = calculateStd(historicalData, avg);
        
        int period = 96;
        
        for (int i = 0; i < forecastPoints; i++) {
            int historicalIndex = i % historicalData.size();
            double historicalValue = historicalData.get(historicalIndex);
            
            int periodIndex = i % period;
            double seasonalFactor = getSeasonalFactor(periodIndex);
            
            double predicted = historicalValue * seasonalFactor * (1 + (random.nextDouble() - 0.5) * 0.1);
            predicted = Math.max(0, predicted);
            
            forecast.add(Math.round(predicted * 100.0) / 100.0);
        }
        
        return forecast;
    }
    
    private double calculateStd(List<Double> data, double mean) {
        double variance = data.stream()
                .mapToDouble(d -> Math.pow(d - mean, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }
    
    private double getSeasonalFactor(int slotIndex) {
        int hour = slotIndex / 4;
        
        if (hour >= 8 && hour < 12) return 1.1;
        if (hour >= 14 && hour < 18) return 1.2;
        if (hour >= 18 && hour < 22) return 1.3;
        if (hour >= 22 || hour < 6) return 0.7;
        return 0.9;
    }
}
