package com.example.demo.service;

import com.example.demo.model.ChargeRequest;
import com.example.demo.model.ChargeResult;
import com.example.demo.model.ElectricityPrice;
import com.example.demo.model.TimePointPrice;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ChargeOptimizationService {

    private static final double TIME_INTERVAL_HOURS = 0.25;

    public ChargeResult calculateOptimalCharge(ChargeRequest request, ElectricityPrice price) {
        ChargeResult result = new ChargeResult();

        if (!validateInput(request, price)) {
            result.setMessage("输入参数验证失败");
            return result;
        }

        double requiredEnergy = request.getRequiredEnergy();
        if (requiredEnergy <= 0) {
            result.setSuccess(true);
            result.setMessage("当前电量已达到或超过目标电量");
            result.setTotalCost(BigDecimal.ZERO);
            result.setTotalEnergy(0.0);
            return result;
        }

        int availableTimePoints = calculateAvailableTimePoints(request);
        double maxPossibleEnergy = availableTimePoints * request.getMaxChargePower() * TIME_INTERVAL_HOURS;

        if (maxPossibleEnergy < requiredEnergy) {
            result.setMessage("充电时间不足，无法在指定时间内完成充电");
            return result;
        }

        List<TimePointPrice> sortedPrices = getSortedPricesInRange(request, price);
        double remainingEnergy = requiredEnergy;

        for (TimePointPrice point : sortedPrices) {
            if (remainingEnergy <= 0) {
                break;
            }

            double maxEnergyAtPoint = request.getMaxChargePower() * TIME_INTERVAL_HOURS;
            double chargeEnergyAtPoint = Math.min(maxEnergyAtPoint, remainingEnergy);
            double chargePower = chargeEnergyAtPoint / TIME_INTERVAL_HOURS;

            result.setChargePower(point.getIndex(), chargePower);
            result.setTotalEnergy(result.getTotalEnergy() + chargeEnergyAtPoint);

            BigDecimal costAtPoint = BigDecimal.valueOf(chargeEnergyAtPoint)
                    .multiply(point.getPrice())
                    .setScale(4, RoundingMode.HALF_UP);
            result.setTotalCost(result.getTotalCost().add(costAtPoint));

            remainingEnergy -= chargeEnergyAtPoint;
        }

        result.setTotalCost(result.getTotalCost().setScale(2, RoundingMode.HALF_UP));
        result.setSuccess(true);
        result.setMessage("充电优化计算完成");
        return result;
    }

    private boolean validateInput(ChargeRequest request, ElectricityPrice price) {
        if (request.getCurrentBattery() < 0 || request.getCurrentBattery() > 100) {
            return false;
        }
        if (request.getTargetBattery() < 0 || request.getTargetBattery() > 100) {
            return false;
        }
        if (request.getStartTimeIndex() < 0 || request.getStartTimeIndex() >= 96) {
            return false;
        }
        if (request.getEndTimeIndex() < 0 || request.getEndTimeIndex() >= 96) {
            return false;
        }
        if (request.getStartTimeIndex() > request.getEndTimeIndex()) {
            return false;
        }
        if (price == null || price.getPrices().size() != 96) {
            return false;
        }
        return true;
    }

    private int calculateAvailableTimePoints(ChargeRequest request) {
        return request.getEndTimeIndex() - request.getStartTimeIndex() + 1;
    }

    private List<TimePointPrice> getSortedPricesInRange(ChargeRequest request, ElectricityPrice price) {
        List<TimePointPrice> pricesInRange = new ArrayList<>();
        for (int i = request.getStartTimeIndex(); i <= request.getEndTimeIndex(); i++) {
            pricesInRange.add(new TimePointPrice(i, price.getPrice(i)));
        }
        pricesInRange.sort(Comparator.comparing(TimePointPrice::getPrice));
        return pricesInRange;
    }

    public ElectricityPrice createDefaultPrice() {
        ElectricityPrice price = new ElectricityPrice();
        for (int i = 0; i < 96; i++) {
            int hour = i / 4;
            BigDecimal priceValue;
            if (hour >= 0 && hour < 6) {
                priceValue = new BigDecimal("0.30");
            } else if (hour >= 6 && hour < 10) {
                priceValue = new BigDecimal("0.80");
            } else if (hour >= 10 && hour < 14) {
                priceValue = new BigDecimal("1.20");
            } else if (hour >= 14 && hour < 18) {
                priceValue = new BigDecimal("0.80");
            } else if (hour >= 18 && hour < 22) {
                priceValue = new BigDecimal("1.20");
            } else {
                priceValue = new BigDecimal("0.50");
            }
            price.setPrice(i, priceValue);
        }
        return price;
    }
}
