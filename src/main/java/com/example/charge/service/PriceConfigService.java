package com.example.charge.service;

import com.example.charge.model.ElectricityPrice;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PriceConfigService {

    private final Map<Integer, ElectricityPrice> priceConfig = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        List<ElectricityPrice> defaultPrices = generateDefaultPrices();
        for (ElectricityPrice price : defaultPrices) {
            priceConfig.put(price.getTimePoint(), price);
        }
    }

    public List<ElectricityPrice> getAllPrices() {
        List<ElectricityPrice> prices = new ArrayList<>();
        for (int i = 0; i < 96; i++) {
            prices.add(priceConfig.get(i));
        }
        return prices;
    }

    public ElectricityPrice getPriceByPoint(int timePoint) {
        return priceConfig.get(timePoint);
    }

    public boolean updatePrice(int timePoint, BigDecimal price, String periodType) {
        if (timePoint < 0 || timePoint >= 96) {
            return false;
        }
        ElectricityPrice newPrice = new ElectricityPrice(timePoint, price, periodType);
        priceConfig.put(timePoint, newPrice);
        return true;
    }

    public boolean updatePrices(List<ElectricityPrice> prices) {
        for (ElectricityPrice price : prices) {
            if (price.getTimePoint() < 0 || price.getTimePoint() >= 96) {
                continue;
            }
            priceConfig.put(price.getTimePoint(), price);
        }
        return true;
    }

    public boolean updatePriceByPeriod(String periodType, BigDecimal price) {
        for (ElectricityPrice p : priceConfig.values()) {
            if (periodType.equals(p.getPeriodType())) {
                p.setPrice(price);
            }
        }
        return true;
    }

    public void resetToDefault() {
        priceConfig.clear();
        List<ElectricityPrice> defaultPrices = generateDefaultPrices();
        for (ElectricityPrice price : defaultPrices) {
            priceConfig.put(price.getTimePoint(), price);
        }
    }

    private List<ElectricityPrice> generateDefaultPrices() {
        List<ElectricityPrice> prices = new ArrayList<>();
        for (int i = 0; i < 96; i++) {
            int hour = i / 4;
            String periodType;
            BigDecimal price;

            if (hour >= 10 && hour < 12) {
                periodType = "尖";
                price = new BigDecimal("1.8");
            } else if (hour >= 18 && hour < 22) {
                periodType = "峰";
                price = new BigDecimal("1.5");
            } else if (hour >= 6 && hour < 10) {
                periodType = "平";
                price = new BigDecimal("0.8");
            } else if (hour >= 12 && hour < 18) {
                periodType = "平";
                price = new BigDecimal("0.8");
            } else if (hour >= 22 && hour < 24) {
                periodType = "谷";
                price = new BigDecimal("0.3");
            } else {
                periodType = "谷";
                price = new BigDecimal("0.3");
            }

            prices.add(new ElectricityPrice(i, price, periodType));
        }
        return prices;
    }
}
