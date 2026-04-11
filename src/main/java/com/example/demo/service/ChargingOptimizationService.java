package com.example.demo.service;

import com.example.demo.model.ChargingRequest;
import com.example.demo.model.ChargingResult;
import com.example.demo.model.ElectricityPriceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
public class ChargingOptimizationService {

    private static final int TOTAL_POINTS = 96;
    private static final BigDecimal POINT_DURATION_HOURS = new BigDecimal("0.25");

    private ElectricityPriceConfig priceConfig = new ElectricityPriceConfig();

    public void setPriceConfig(ElectricityPriceConfig config) {
        this.priceConfig = config;
    }

    public ElectricityPriceConfig getPriceConfig() {
        return this.priceConfig;
    }

    public BigDecimal[] get96PointPrices() {
        BigDecimal[] prices = new BigDecimal[TOTAL_POINTS];
        for (int i = 0; i < TOTAL_POINTS; i++) {
            prices[i] = getPriceByPoint(i);
        }
        return prices;
    }

    public String[] get96PointPriceTypes() {
        String[] types = new String[TOTAL_POINTS];
        for (int i = 0; i < TOTAL_POINTS; i++) {
            types[i] = getPriceTypeByPoint(i);
        }
        return types;
    }

    public BigDecimal getPriceByPoint(int point) {
        int hour = point / 4;
        return getPriceByHour(hour);
    }

    public String getPriceTypeByPoint(int point) {
        int hour = point / 4;
        return getPriceTypeByHour(hour);
    }

    private BigDecimal getPriceByHour(int hour) {
        if (hour >= 19 && hour < 21) {
            return priceConfig.getSharpPeakPrice();
        } else if ((hour >= 8 && hour < 11) || (hour >= 16 && hour < 19)) {
            return priceConfig.getPeakPrice();
        } else if ((hour >= 7 && hour < 8) || (hour >= 11 && hour < 16) || (hour >= 21 && hour < 23)) {
            return priceConfig.getFlatPrice();
        } else {
            return priceConfig.getValleyPrice();
        }
    }

    private String getPriceTypeByHour(int hour) {
        if (hour >= 19 && hour < 21) {
            return "尖峰";
        } else if ((hour >= 8 && hour < 11) || (hour >= 16 && hour < 19)) {
            return "高峰";
        } else if ((hour >= 7 && hour < 8) || (hour >= 11 && hour < 16) || (hour >= 21 && hour < 23)) {
            return "平价";
        } else {
            return "低谷";
        }
    }

    public String getTimeSlotByPoint(int point) {
        int hour = point / 4;
        int minute = (point % 4) * 15;
        return String.format("%02d:%02d", hour, minute);
    }

    public ChargingResult optimizeCharging(ChargingRequest request) {
        log.info("开始优化充电策略，请求参数: {}", request);
        
        validateRequest(request);
        
        BigDecimal energyNeeded = calculateEnergyNeeded(request);
        log.info("需要充电能量: {} kWh", energyNeeded);
        
        if (energyNeeded.compareTo(BigDecimal.ZERO) <= 0) {
            ChargingResult result = new ChargingResult();
            result.setMessage("当前电量已达到或超过目标电量，无需充电");
            result.setTotalEnergy(BigDecimal.ZERO);
            result.setTotalCost(BigDecimal.ZERO);
            result.setChargingPoints(new ArrayList<>());
            return result;
        }
        
        int startPoint = request.getStartPoint();
        int endPoint = request.getEndPoint();
        int availablePoints = endPoint - startPoint + 1;
        
        BigDecimal maxEnergyPerPoint = request.getMaxChargingPower().multiply(POINT_DURATION_HOURS);
        BigDecimal maxTotalEnergy = maxEnergyPerPoint.multiply(new BigDecimal(availablePoints));
        
        if (maxTotalEnergy.compareTo(energyNeeded) < 0) {
            ChargingResult result = new ChargingResult();
            result.setMessage(String.format("充电时间不足，最大可充电量 %.2f kWh，需要充电量 %.2f kWh", 
                    maxTotalEnergy, energyNeeded));
            result.setTotalEnergy(BigDecimal.ZERO);
            result.setTotalCost(BigDecimal.ZERO);
            result.setChargingPoints(new ArrayList<>());
            return result;
        }
        
        return calculateOptimalSchedule(request, energyNeeded, startPoint, endPoint);
    }

    private void validateRequest(ChargingRequest request) {
        if (request.getStartPoint() == null || request.getEndPoint() == null) {
            throw new IllegalArgumentException("充电开始点和结束点不能为空");
        }
        if (request.getStartPoint() < 0 || request.getStartPoint() >= TOTAL_POINTS) {
            throw new IllegalArgumentException("充电开始点必须在0-95之间");
        }
        if (request.getEndPoint() < 0 || request.getEndPoint() >= TOTAL_POINTS) {
            throw new IllegalArgumentException("充电结束点必须在0-95之间");
        }
        if (request.getStartPoint() > request.getEndPoint()) {
            throw new IllegalArgumentException("充电开始点不能大于结束点");
        }
        if (request.getCurrentSoc() == null || request.getTargetSoc() == null) {
            throw new IllegalArgumentException("当前电量和目标电量不能为空");
        }
        if (request.getCurrentSoc().compareTo(BigDecimal.ZERO) < 0 || 
            request.getCurrentSoc().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("当前电量必须在0-100之间");
        }
        if (request.getTargetSoc().compareTo(BigDecimal.ZERO) < 0 || 
            request.getTargetSoc().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("目标电量必须在0-100之间");
        }
    }

    private BigDecimal calculateEnergyNeeded(ChargingRequest request) {
        BigDecimal socDifference = request.getTargetSoc().subtract(request.getCurrentSoc());
        return socDifference.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
                .multiply(request.getBatteryCapacity());
    }

    private ChargingResult calculateOptimalSchedule(ChargingRequest request, BigDecimal energyNeeded, 
                                                     int startPoint, int endPoint) {
        List<PointPrice> pointPrices = new ArrayList<>();
        for (int i = startPoint; i <= endPoint; i++) {
            pointPrices.add(new PointPrice(i, getPriceByPoint(i), getPriceTypeByPoint(i)));
        }
        
        pointPrices.sort(Comparator.comparing(PointPrice::getPrice));
        
        BigDecimal remainingEnergy = energyNeeded;
        BigDecimal[] powerSchedule = new BigDecimal[TOTAL_POINTS];
        Arrays.fill(powerSchedule, BigDecimal.ZERO);
        
        for (PointPrice pp : pointPrices) {
            if (remainingEnergy.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            
            BigDecimal maxEnergyPerPoint = request.getMaxChargingPower().multiply(POINT_DURATION_HOURS);
            BigDecimal energyToCharge = remainingEnergy.min(maxEnergyPerPoint);
            
            powerSchedule[pp.getPoint()] = energyToCharge.divide(POINT_DURATION_HOURS, 4, RoundingMode.HALF_UP);
            remainingEnergy = remainingEnergy.subtract(energyToCharge);
        }
        
        return buildResult(powerSchedule, startPoint, endPoint, request);
    }

    private ChargingResult buildResult(BigDecimal[] powerSchedule, int startPoint, int endPoint, 
                                        ChargingRequest request) {
        List<ChargingResult.ChargingPoint> chargingPoints = new ArrayList<>();
        BigDecimal totalEnergy = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        int actualStart = -1;
        int actualEnd = -1;
        
        for (int i = startPoint; i <= endPoint; i++) {
            BigDecimal power = powerSchedule[i];
            if (power.compareTo(BigDecimal.ZERO) > 0) {
                if (actualStart == -1) {
                    actualStart = i;
                }
                actualEnd = i;
                
                BigDecimal energy = power.multiply(POINT_DURATION_HOURS);
                BigDecimal price = getPriceByPoint(i);
                BigDecimal cost = energy.multiply(price);
                
                totalEnergy = totalEnergy.add(energy);
                totalCost = totalCost.add(cost);
                
                ChargingResult.ChargingPoint cp = new ChargingResult.ChargingPoint(
                    i, 
                    getTimeSlotByPoint(i),
                    price,
                    getPriceTypeByPoint(i),
                    power,
                    energy,
                    cost
                );
                chargingPoints.add(cp);
            }
        }
        
        chargingPoints.sort(Comparator.comparing(ChargingResult.ChargingPoint::getPointIndex));
        
        ChargingResult result = new ChargingResult();
        result.setChargingPoints(chargingPoints);
        result.setTotalEnergy(totalEnergy.setScale(4, RoundingMode.HALF_UP));
        result.setTotalCost(totalCost.setScale(4, RoundingMode.HALF_UP));
        result.setChargingDuration(chargingPoints.size());
        result.setActualStartPoint(actualStart != -1 ? new BigDecimal(actualStart) : null);
        result.setActualEndPoint(actualEnd != -1 ? new BigDecimal(actualEnd) : null);
        result.setMessage("充电优化成功");
        
        log.info("充电优化完成，总能量: {} kWh，总成本: {} 元", totalEnergy, totalCost);
        
        return result;
    }

    private static class PointPrice {
        private final int point;
        private final BigDecimal price;
        private final String priceType;

        public PointPrice(int point, BigDecimal price, String priceType) {
            this.point = point;
            this.price = price;
            this.priceType = priceType;
        }

        public int getPoint() {
            return point;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public String getPriceType() {
            return priceType;
        }
    }
}
