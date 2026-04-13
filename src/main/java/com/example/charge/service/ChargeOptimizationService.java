package com.example.charge.service;

import com.example.charge.model.ChargeRequest;
import com.example.charge.model.ChargeResult;
import com.example.charge.model.ElectricityPrice;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChargeOptimizationService {

    public ChargeResult calculateOptimalCharge(ChargeRequest request) {
        ChargeResult result = new ChargeResult();

        if (!validateRequest(request)) {
            return new ChargeResult(false, "请求参数验证失败");
        }

        int batteryNeeded = request.getTargetBattery() - request.getCurrentBattery();
        if (batteryNeeded <= 0) {
            return new ChargeResult(false, "当前电量已大于或等于目标电量");
        }

        BigDecimal totalEnergyNeeded = request.getBatteryCapacity()
                .multiply(new BigDecimal(batteryNeeded))
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

        List<ElectricityPrice> validPrices = request.getPriceList().stream()
                .filter(p -> p.getTimePoint() >= request.getStartTimePoint()
                        && p.getTimePoint() < request.getEndTimePoint())
                .sorted(Comparator.comparing(ElectricityPrice::getPrice))
                .collect(Collectors.toList());

        if (validPrices.isEmpty()) {
            return new ChargeResult(false, "指定时间范围内无有效电价数据");
        }

        int availableTimeSlots = request.getEndTimePoint() - request.getStartTimePoint();
        BigDecimal maxPossibleEnergy = request.getMaxChargePower()
                .multiply(new BigDecimal("0.25"))
                .multiply(new BigDecimal(availableTimeSlots));

        if (maxPossibleEnergy.compareTo(totalEnergyNeeded) < 0) {
            return new ChargeResult(false, "可用时间不足以完成充电");
        }

        Map<Integer, BigDecimal> powerPerPoint = new HashMap<>();
        BigDecimal remainingEnergy = totalEnergyNeeded;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (ElectricityPrice price : validPrices) {
            if (remainingEnergy.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal maxEnergyPerSlot = request.getMaxChargePower().multiply(new BigDecimal("0.25"));
            BigDecimal energyToCharge = remainingEnergy.min(maxEnergyPerSlot);

            BigDecimal power = energyToCharge.divide(new BigDecimal("0.25"), 4, RoundingMode.HALF_UP);
            powerPerPoint.put(price.getTimePoint(), power);

            BigDecimal cost = energyToCharge.multiply(price.getPrice());
            totalCost = totalCost.add(cost);

            remainingEnergy = remainingEnergy.subtract(energyToCharge);
        }

        for (int i = request.getStartTimePoint(); i < request.getEndTimePoint(); i++) {
            powerPerPoint.putIfAbsent(i, BigDecimal.ZERO);
        }

        List<String> schedule = buildChargeSchedule(powerPerPoint, request.getPriceList());

        result.setSuccess(true);
        result.setMessage("计算成功");
        result.setTotalCost(totalCost.setScale(2, RoundingMode.HALF_UP));
        result.setTotalEnergy(totalEnergyNeeded.setScale(2, RoundingMode.HALF_UP));
        result.setPowerPerPoint(powerPerPoint);
        result.setChargeSchedule(schedule);

        return result;
    }

    private boolean validateRequest(ChargeRequest request) {
        if (request.getCurrentBattery() < 0 || request.getCurrentBattery() > 100) {
            return false;
        }
        if (request.getTargetBattery() < 0 || request.getTargetBattery() > 100) {
            return false;
        }
        if (request.getStartTimePoint() < 0 || request.getStartTimePoint() >= 96) {
            return false;
        }
        if (request.getEndTimePoint() <= 0 || request.getEndTimePoint() > 96) {
            return false;
        }
        if (request.getStartTimePoint() >= request.getEndTimePoint()) {
            return false;
        }
        return request.getPriceList() != null && !request.getPriceList().isEmpty();
    }

    private List<String> buildChargeSchedule(Map<Integer, BigDecimal> powerPerPoint,
                                             List<ElectricityPrice> priceList) {
        List<String> schedule = new ArrayList<>();
        Map<Integer, ElectricityPrice> priceMap = new HashMap<>();
        for (ElectricityPrice price : priceList) {
            priceMap.put(price.getTimePoint(), price);
        }

        List<Integer> sortedPoints = new ArrayList<>(powerPerPoint.keySet());
        Collections.sort(sortedPoints);

        for (Integer point : sortedPoints) {
            BigDecimal power = powerPerPoint.get(point);
            ElectricityPrice price = priceMap.get(point);
            if (power.compareTo(BigDecimal.ZERO) > 0) {
                String timeStr = formatTime(point);
                schedule.add(String.format("时间点%d(%s) - 充电功率: %.2f kW, 电价: %.4f 元/kWh [%s]",
                        point, timeStr, power,
                        price != null ? price.getPrice() : BigDecimal.ZERO,
                        price != null ? price.getPeriodType() : "未知"));
            }
        }

        return schedule;
    }

    private String formatTime(int point) {
        int totalMinutes = point * 15;
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    public List<ElectricityPrice> generateDefaultPriceList() {
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
