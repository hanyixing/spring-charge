package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChargingRecommendationService {
    
    private static final int TIME_SLOTS = 96;
    
    private static final double HABIT_WEIGHT = 0.4;
    private static final double PEAK_VALLEY_WEIGHT = 0.3;
    private static final double PRICE_WEIGHT = 0.3;
    
    private static final double EPSILON = 1e-6;
    
    private static final double DEFAULT_BATTERY_CAPACITY = 60.0;
    private static final double DEFAULT_SOC_MIN = 10.0;
    private static final double DEFAULT_SOC_MAX = 95.0;

    public List<Double> recommend(
            List<Double> historicalCurve,
            List<Double> priceCurve,
            List<Double> gridLoadCurve,
            double totalDemand,
            double maxPower,
            double minPower) {
        
        return recommendWithSoc(historicalCurve, priceCurve, gridLoadCurve,
                totalDemand, maxPower, minPower,
                0, 100, DEFAULT_BATTERY_CAPACITY, DEFAULT_SOC_MIN, DEFAULT_SOC_MAX, TIME_SLOTS);
    }
    
    public List<Double> recommendWithSoc(
            List<Double> historicalCurve,
            List<Double> priceCurve,
            List<Double> gridLoadCurve,
            double totalDemand,
            double maxPower,
            double minPower,
            double currentSoc,
            double targetSoc,
            double batteryCapacity,
            double socMin,
            double socMax,
            int availableTimeSlots) {
        
        log.info("开始充电推荐算法计算（含SOC约束）...");
        log.info("总需求: {} kWh, 最大功率: {} kW, 最小功率: {} kW", totalDemand, maxPower, minPower);
        log.info("SOC: {}% -> {}%, 电池容量: {} kWh, SOC约束: [{}, {}]", 
                currentSoc, targetSoc, batteryCapacity, socMin, socMax);
        
        if (batteryCapacity <= 0) {
            batteryCapacity = DEFAULT_BATTERY_CAPACITY;
        }
        if (socMin < 0) socMin = DEFAULT_SOC_MIN;
        if (socMax > 100) socMax = DEFAULT_SOC_MAX;
        if (availableTimeSlots <= 0 || availableTimeSlots > TIME_SLOTS) {
            availableTimeSlots = TIME_SLOTS;
        }
        
        double socDemand = (targetSoc - currentSoc) / 100.0 * batteryCapacity;
        double actualDemand = totalDemand > 0 ? Math.min(totalDemand, socDemand) : socDemand;
        
        if (actualDemand <= 0) {
            log.info("无需充电，当前SOC已达到或超过目标SOC");
            List<Double> zeroCurve = new ArrayList<>();
            for (int i = 0; i < TIME_SLOTS; i++) {
                zeroCurve.add(0.0);
            }
            return zeroCurve;
        }
        
        log.info("实际充电需求: {} kWh", actualDemand);
        
        List<Double> habitWeights = calculateHabitWeights(historicalCurve);
        log.debug("习惯权重计算完成");
        
        List<Double> peakValleyWeights = calculatePeakValleyWeights(gridLoadCurve);
        log.debug("削峰平谷权重计算完成");
        
        List<Double> priceWeights = calculatePriceWeights(priceCurve);
        log.debug("电价权重计算完成");
        
        List<Double> combinedWeights = combineWeights(habitWeights, peakValleyWeights, priceWeights);
        log.debug("综合权重计算完成");
        
        List<Double> recommendedCurve = optimizeAllocationWithSoc(
                combinedWeights, actualDemand, maxPower, minPower,
                currentSoc, batteryCapacity, socMin, socMax, availableTimeSlots);
        
        log.info("充电推荐算法计算完成");
        return recommendedCurve;
    }
    
    private List<Double> optimizeAllocationWithSoc(
            List<Double> weights,
            double totalDemand,
            double maxPower,
            double minPower,
            double currentSoc,
            double batteryCapacity,
            double socMin,
            double socMax,
            int availableTimeSlots) {
        
        List<Double> result = new ArrayList<>();
        double[] allocation = new double[TIME_SLOTS];
        double remainingDemand = totalDemand;
        double currentSocValue = currentSoc;
        
        double maxEnergyPerSlot = maxPower * 0.25;
        double socChangePerKwh = 100.0 / batteryCapacity;
        
        java.util.List<Integer> sortedIndices = new java.util.ArrayList<>();
        for (int i = 0; i < availableTimeSlots; i++) {
            sortedIndices.add(i);
        }
        sortedIndices.sort((a, b) -> Double.compare(weights.get(b), weights.get(a)));
        
        for (int idx : sortedIndices) {
            if (remainingDemand <= EPSILON) {
                allocation[idx] = 0;
                continue;
            }
            
            double maxEnergy = Math.min(maxEnergyPerSlot, remainingDemand);
            double maxSocIncrease = socMax - currentSocValue;
            double maxEnergyBySoc = maxSocIncrease / socChangePerKwh;
            maxEnergy = Math.min(maxEnergy, maxEnergyBySoc);
            
            if (maxEnergy <= EPSILON) {
                allocation[idx] = 0;
                continue;
            }
            
            allocation[idx] = maxEnergy;
            remainingDemand -= maxEnergy;
            currentSocValue += maxEnergy * socChangePerKwh;
        }
        
        for (int i = 0; i < TIME_SLOTS; i++) {
            result.add(allocation[i]);
        }
        
        return result;
    }
    
    public List<Double> calculateSocCurve(List<Double> chargingCurve, 
            double initialSoc, double batteryCapacity) {
        
        List<Double> socCurve = new ArrayList<>();
        double currentSoc = initialSoc;
        double socChangePerKwh = 100.0 / batteryCapacity;
        
        for (int i = 0; i < TIME_SLOTS; i++) {
            socCurve.add(currentSoc);
            double energy = chargingCurve.get(i);
            currentSoc += energy * socChangePerKwh;
            currentSoc = Math.min(100.0, currentSoc);
        }
        socCurve.add(currentSoc);
        
        return socCurve;
    }
    
    private List<Double> calculateHabitWeights(List<Double> historicalCurve) {
        List<Double> weights = new ArrayList<>();
        double sum = 0.0;
        
        for (int i = 0; i < TIME_SLOTS; i++) {
            double value = historicalCurve != null && i < historicalCurve.size() 
                    ? historicalCurve.get(i) : 0.0;
            value = Math.max(0, value);
            weights.add(value);
            sum += value;
        }
        
        if (sum < EPSILON) {
            for (int i = 0; i < TIME_SLOTS; i++) {
                weights.set(i, 1.0 / TIME_SLOTS);
            }
        } else {
            for (int i = 0; i < TIME_SLOTS; i++) {
                weights.set(i, weights.get(i) / sum);
            }
        }
        
        return weights;
    }
    
    private List<Double> calculatePeakValleyWeights(List<Double> gridLoadCurve) {
        List<Double> weights = new ArrayList<>();
        
        double maxLoad = 0.0;
        for (int i = 0; i < TIME_SLOTS; i++) {
            double load = gridLoadCurve != null && i < gridLoadCurve.size() 
                    ? gridLoadCurve.get(i) : 0.5;
            maxLoad = Math.max(maxLoad, load);
        }
        
        maxLoad = Math.max(maxLoad, EPSILON);
        
        for (int i = 0; i < TIME_SLOTS; i++) {
            double load = gridLoadCurve != null && i < gridLoadCurve.size() 
                    ? gridLoadCurve.get(i) : 0.5;
            double weight = 1.0 - (load / maxLoad);
            weights.add(weight);
        }
        
        double sum = weights.stream().mapToDouble(Double::doubleValue).sum();
        if (sum < EPSILON) {
            for (int i = 0; i < TIME_SLOTS; i++) {
                weights.set(i, 1.0 / TIME_SLOTS);
            }
        } else {
            for (int i = 0; i < TIME_SLOTS; i++) {
                weights.set(i, weights.get(i) / sum);
            }
        }
        
        return weights;
    }
    
    private List<Double> calculatePriceWeights(List<Double> priceCurve) {
        List<Double> weights = new ArrayList<>();
        
        double maxPrice = 0.0;
        for (int i = 0; i < TIME_SLOTS; i++) {
            double price = priceCurve != null && i < priceCurve.size() 
                    ? priceCurve.get(i) : 1.0;
            maxPrice = Math.max(maxPrice, price);
        }
        
        maxPrice = Math.max(maxPrice, EPSILON);
        
        for (int i = 0; i < TIME_SLOTS; i++) {
            double price = priceCurve != null && i < priceCurve.size() 
                    ? priceCurve.get(i) : 1.0;
            double weight = 1.0 - (price / maxPrice);
            weights.add(weight);
        }
        
        double sum = weights.stream().mapToDouble(Double::doubleValue).sum();
        if (sum < EPSILON) {
            for (int i = 0; i < TIME_SLOTS; i++) {
                weights.set(i, 1.0 / TIME_SLOTS);
            }
        } else {
            for (int i = 0; i < TIME_SLOTS; i++) {
                weights.set(i, weights.get(i) / sum);
            }
        }
        
        return weights;
    }
    
    private List<Double> combineWeights(
            List<Double> habitWeights,
            List<Double> peakValleyWeights,
            List<Double> priceWeights) {
        
        List<Double> combined = new ArrayList<>();
        
        for (int i = 0; i < TIME_SLOTS; i++) {
            double weight = HABIT_WEIGHT * habitWeights.get(i)
                    + PEAK_VALLEY_WEIGHT * peakValleyWeights.get(i)
                    + PRICE_WEIGHT * priceWeights.get(i);
            combined.add(weight);
        }
        
        double sum = combined.stream().mapToDouble(Double::doubleValue).sum();
        if (sum < EPSILON) {
            for (int i = 0; i < TIME_SLOTS; i++) {
                combined.set(i, 1.0 / TIME_SLOTS);
            }
        } else {
            for (int i = 0; i < TIME_SLOTS; i++) {
                combined.set(i, combined.get(i) / sum);
            }
        }
        
        return combined;
    }
    
    private List<Double> optimizeAllocation(
            List<Double> weights,
            double totalDemand,
            double maxPower,
            double minPower) {
        
        List<Double> result = new ArrayList<>();
        
        double slotEnergy = maxPower * 0.25;
        double maxSlotsNeeded = totalDemand / slotEnergy;
        
        double[] allocation = new double[TIME_SLOTS];
        double remainingDemand = totalDemand;
        
        java.util.List<Integer> sortedIndices = new java.util.ArrayList<>();
        for (int i = 0; i < TIME_SLOTS; i++) {
            sortedIndices.add(i);
        }
        sortedIndices.sort((a, b) -> Double.compare(weights.get(b), weights.get(a)));
        
        for (int idx : sortedIndices) {
            if (remainingDemand <= EPSILON) {
                allocation[idx] = 0;
                continue;
            }
            
            double maxEnergy = maxPower * 0.25;
            double allocated = Math.min(maxEnergy, remainingDemand);
            allocation[idx] = allocated;
            remainingDemand -= allocated;
        }
        
        for (int i = 0; i < TIME_SLOTS; i++) {
            result.add(allocation[i]);
        }
        
        return result;
    }
    
    public double calculateTotalCost(List<Double> curve, List<Double> priceCurve) {
        double cost = 0.0;
        for (int i = 0; i < TIME_SLOTS; i++) {
            double energy = curve.get(i);
            double price = priceCurve != null && i < priceCurve.size() 
                    ? priceCurve.get(i) : 1.0;
            cost += energy * price;
        }
        return cost;
    }
    
    public double calculatePeakValleyScore(List<Double> curve, List<Double> gridLoadCurve) {
        double score = 0.0;
        double totalEnergy = curve.stream().mapToDouble(Double::doubleValue).sum();
        
        if (totalEnergy < EPSILON) {
            return 0.0;
        }
        
        double maxLoad = 0.0;
        for (int i = 0; i < TIME_SLOTS; i++) {
            double load = gridLoadCurve != null && i < gridLoadCurve.size() 
                    ? gridLoadCurve.get(i) : 0.5;
            maxLoad = Math.max(maxLoad, load);
        }
        maxLoad = Math.max(maxLoad, EPSILON);
        
        for (int i = 0; i < TIME_SLOTS; i++) {
            double energy = curve.get(i);
            double load = gridLoadCurve != null && i < gridLoadCurve.size() 
                    ? gridLoadCurve.get(i) : 0.5;
            double normalizedLoad = load / maxLoad;
            score += energy * (1.0 - normalizedLoad);
        }
        
        return score / totalEnergy;
    }
    
    public double calculateHabitMatchScore(List<Double> curve, List<Double> historicalCurve) {
        if (historicalCurve == null || historicalCurve.isEmpty()) {
            return 0.0;
        }
        
        double sumCurve = curve.stream().mapToDouble(Double::doubleValue).sum();
        double sumHistory = historicalCurve.stream().mapToDouble(Double::doubleValue).sum();
        
        if (sumCurve < EPSILON || sumHistory < EPSILON) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double normCurve = 0.0;
        double normHistory = 0.0;
        
        for (int i = 0; i < TIME_SLOTS; i++) {
            double c = curve.get(i) / sumCurve;
            double h = (i < historicalCurve.size() ? historicalCurve.get(i) : 0) / sumHistory;
            dotProduct += c * h;
            normCurve += c * c;
            normHistory += h * h;
        }
        
        double denominator = Math.sqrt(normCurve) * Math.sqrt(normHistory);
        if (denominator < EPSILON) {
            return 0.0;
        }
        
        return dotProduct / denominator;
    }
}
