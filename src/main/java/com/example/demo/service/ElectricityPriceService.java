package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ElectricityPriceService {
    
    private static final double BASE_PRICE = 0.5;
    private static final double PEAK_MULTIPLIER = 1.5;
    private static final double VALLEY_MULTIPLIER = 0.4;
    private static final double NORMAL_MULTIPLIER = 1.0;
    
    public List<Double> getElectricityPrices() {
        log.info("获取96个时间点的电价数据");
        
        List<Double> prices = new ArrayList<>();
        
        for (int i = 0; i < 96; i++) {
            double price = calculatePrice(i);
            prices.add(price);
        }
        
        log.info("电价数据获取完成，峰时电价: {}, 谷时电价: {}", 
                prices.stream().max(Double::compare).orElse(0.0),
                prices.stream().min(Double::compare).orElse(0.0));
        
        return prices;
    }
    
    private double calculatePrice(int slotIndex) {
        int hour = slotIndex / 4;
        double multiplier;
        
        if (hour >= 8 && hour < 12) {
            multiplier = PEAK_MULTIPLIER;
        } else if (hour >= 14 && hour < 18) {
            multiplier = PEAK_MULTIPLIER;
        } else if (hour >= 18 && hour < 22) {
            multiplier = PEAK_MULTIPLIER * 1.2;
        } else if (hour >= 23 || hour < 7) {
            multiplier = VALLEY_MULTIPLIER;
        } else {
            multiplier = NORMAL_MULTIPLIER;
        }
        
        return Math.round(BASE_PRICE * multiplier * 1000.0) / 1000.0;
    }
    
    public String getPricePeriod(int slotIndex) {
        int hour = slotIndex / 4;
        
        if (hour >= 8 && hour < 12) return "峰时";
        if (hour >= 14 && hour < 18) return "峰时";
        if (hour >= 18 && hour < 22) return "尖峰";
        if (hour >= 23 || hour < 7) return "谷时";
        return "平时";
    }
    
    public boolean isValleyPeriod(int slotIndex) {
        int hour = slotIndex / 4;
        return hour >= 23 || hour < 7;
    }
    
    public boolean isPeakPeriod(int slotIndex) {
        int hour = slotIndex / 4;
        return (hour >= 8 && hour < 12) || (hour >= 14 && hour < 22);
    }
}
