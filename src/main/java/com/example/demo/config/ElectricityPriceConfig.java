package com.example.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "electricity.price")
public class ElectricityPriceConfig {

    private double sharpPrice = 1.2;
    private double peakPrice = 1.0;
    private double flatPrice = 0.7;
    private double valleyPrice = 0.4;

    private Map<Integer, String> timePeriodMap = new HashMap<>();

    public ElectricityPriceConfig() {
        initDefaultTimePeriods();
    }

    private void initDefaultTimePeriods() {
        for (int i = 0; i < 96; i++) {
            int hour = i / 4;
            if (hour >= 10 && hour < 12) {
                timePeriodMap.put(i, "sharp");
            } else if (hour >= 14 && hour < 17) {
                timePeriodMap.put(i, "sharp");
            } else if (hour >= 19 && hour < 21) {
                timePeriodMap.put(i, "sharp");
            } else if (hour >= 8 && hour < 10) {
                timePeriodMap.put(i, "peak");
            } else if (hour >= 12 && hour < 14) {
                timePeriodMap.put(i, "peak");
            } else if (hour >= 17 && hour < 19) {
                timePeriodMap.put(i, "peak");
            } else if (hour >= 21 && hour < 23) {
                timePeriodMap.put(i, "peak");
            } else if (hour >= 7 && hour < 8) {
                timePeriodMap.put(i, "flat");
            } else if (hour >= 23 || hour < 7) {
                timePeriodMap.put(i, "valley");
            } else {
                timePeriodMap.put(i, "flat");
            }
        }
    }

    public double getPriceByPoint(int point) {
        String period = timePeriodMap.get(point % 96);
        switch (period) {
            case "sharp":
                return sharpPrice;
            case "peak":
                return peakPrice;
            case "valley":
                return valleyPrice;
            default:
                return flatPrice;
        }
    }

    public String getPeriodByPoint(int point) {
        return timePeriodMap.get(point % 96);
    }

    public void setTimePeriod(int point, String period) {
        timePeriodMap.put(point, period);
    }
}
