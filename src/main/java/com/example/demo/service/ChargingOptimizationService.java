package com.example.demo.service;

import com.example.demo.config.ElectricityPriceConfig;
import com.example.demo.dto.ChargingRequest;
import com.example.demo.dto.ChargingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChargingOptimizationService {

    private final ElectricityPriceConfig priceConfig;

    public ChargingResponse optimizeCharging(ChargingRequest request) {
        log.info("开始优化充电，请求参数: {}", request);

        if (request.getCurrentSoc() >= request.getTargetSoc()) {
            return ChargingResponse.builder()
                    .success(false)
                    .message("当前电量已达到或超过目标电量，无需充电")
                    .build();
        }

        if (request.getStartPoint() > request.getEndPoint()) {
            return ChargingResponse.builder()
                    .success(false)
                    .message("开始时间点不能大于结束时间点")
                    .build();
        }

        double energyNeeded = (request.getTargetSoc() - request.getCurrentSoc()) / 100.0 * request.getBatteryCapacity();
        log.info("需要充电电量: {} kWh", energyNeeded);

        List<Integer> availablePoints = getAvailablePoints(request.getStartPoint(), request.getEndPoint());
        log.info("可用时间点数量: {}", availablePoints.size());

        List<PointPrice> pointPrices = availablePoints.stream()
                .map(point -> new PointPrice(point, priceConfig.getPriceByPoint(point), priceConfig.getPeriodByPoint(point)))
                .sorted(Comparator.comparingDouble(PointPrice::getPrice))
                .collect(Collectors.toList());

        double energyPerPoint = request.getMaxChargingPower() * 0.25;
        int pointsNeeded = (int) Math.ceil(energyNeeded / energyPerPoint);
        log.info("需要的充电点数: {}", pointsNeeded);

        if (pointsNeeded > availablePoints.size()) {
            return ChargingResponse.builder()
                    .success(false)
                    .message(String.format("充电时间不足，需要至少%d个时间点，但只有%d个时间点可用", 
                            pointsNeeded, availablePoints.size()))
                    .build();
        }

        List<ChargingResponse.ChargingPoint> schedule = new ArrayList<>();
        double totalEnergy = 0.0;
        double totalCost = 0.0;
        double currentSoc = request.getCurrentSoc();

        for (int i = 0; i < pointPrices.size() && totalEnergy < energyNeeded; i++) {
            PointPrice pp = pointPrices.get(i);
            
            double remainingEnergy = energyNeeded - totalEnergy;
            double pointEnergy = Math.min(energyPerPoint, remainingEnergy);
            double pointPower = pointEnergy / 0.25;
            
            double pointCost = pointEnergy * pp.getPrice();
            
            currentSoc += (pointEnergy / request.getBatteryCapacity()) * 100;
            
            ChargingResponse.ChargingPoint cp = ChargingResponse.ChargingPoint.builder()
                    .point(pp.getPoint())
                    .hour(pp.getPoint() / 4)
                    .minute((pp.getPoint() % 4) * 15)
                    .period(pp.getPeriod())
                    .price(pp.getPrice())
                    .power(pointPower)
                    .energy(pointEnergy)
                    .cost(pointCost)
                    .soc(Math.min(100.0, currentSoc))
                    .build();
            
            schedule.add(cp);
            totalEnergy += pointEnergy;
            totalCost += pointCost;
        }

        schedule.sort(Comparator.comparingInt(ChargingResponse.ChargingPoint::getPoint));

        log.info("充电优化完成，总电量: {} kWh，总成本: {} 元", totalEnergy, totalCost);

        return ChargingResponse.builder()
                .success(true)
                .message("充电优化成功")
                .totalEnergy(Math.round(totalEnergy * 100.0) / 100.0)
                .totalCost(Math.round(totalCost * 100.0) / 100.0)
                .chargingPoints(schedule.size())
                .chargingSchedule(schedule)
                .actualStartSoc(request.getCurrentSoc())
                .actualEndSoc(Math.round(currentSoc * 100.0) / 100.0)
                .build();
    }

    private List<Integer> getAvailablePoints(Integer start, Integer end) {
        List<Integer> points = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            points.add(i);
        }
        return points;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class PointPrice {
        private int point;
        private double price;
        private String period;
    }
}
