package com.example.charge.service;

import com.example.charge.model.ElectricityPrice;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ElectricityPriceService {

    public List<ElectricityPrice> generateDayAheadPrices() {
        List<ElectricityPrice> prices = new ArrayList<>();
        
        for (int i = 0; i < 96; i++) {
            int hour = i / 4;
            String periodType = getPeriodType(hour);
            BigDecimal price = getPriceByPeriod(periodType);
            
            prices.add(new ElectricityPrice(i, price, periodType));
        }
        
        return prices;
    }

    private String getPeriodType(int hour) {
        if (hour >= 0 && hour < 6) {
            return "VALLEY";
        } else if (hour >= 6 && hour < 10) {
            return "PEAK";
        } else if (hour >= 10 && hour < 14) {
            return "MID";
        } else if (hour >= 14 && hour < 18) {
            return "PEAK";
        } else if (hour >= 18 && hour < 22) {
            return "MID";
        } else {
            return "VALLEY";
        }
    }

    private BigDecimal getPriceByPeriod(String periodType) {
        switch (periodType) {
            case "VALLEY":
                return new BigDecimal("0.35");
            case "MID":
                return new BigDecimal("0.68");
            case "PEAK":
                return new BigDecimal("1.25");
            default:
                return new BigDecimal("0.68");
        }
    }
}
