package com.example.demo.service;

import com.example.demo.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 充放电优化调度服务
 * 核心算法: 基于预测的动态规划优化
 */
@Service
public class ChargeDischargeOptimizationService {

    @Autowired
    private DemandForecastService forecastService;

    private static final int TIME_POINTS = 96;
    private static final double TIME_INTERVAL_HOURS = 0.25;
    private static final double EPSILON = 1e-6;

    /**
     * 执行优化调度
     */
    public OptimizationResult optimize(List<StorageDevice> devices, 
                                       LocalDateTime startTime,
                                       List<double[]> historicalData) {
        long startOptimizationTime = System.currentTimeMillis();
        
        OptimizationResult result = new OptimizationResult();
        
        // 1. 生成需求预测
        List<DemandForecast> forecasts = forecastService.generateForecast(startTime, historicalData);
        
        // 2. 计算总容量和约束
        double totalMaxChargePower = devices.stream()
                .mapToDouble(StorageDevice::getMaxChargePower)
                .sum();
        double totalMaxDischargePower = devices.stream()
                .mapToDouble(StorageDevice::getMaxDischargePower)
                .sum();
        double totalCapacity = devices.stream()
                .mapToDouble(StorageDevice::getRatedCapacity)
                .sum();
        double initialTotalSoc = devices.stream()
                .mapToDouble(StorageDevice::getCurrentSoc)
                .average()
                .orElse(0.5);
        
        // 3. 使用动态规划求解最优调度
        List<PowerSchedule> optimalSchedule = solveDynamicProgramming(
                forecasts, devices, totalMaxChargePower, totalMaxDischargePower, 
                totalCapacity, initialTotalSoc);
        
        // 4. 为每个设备分配功率
        Map<String, List<PowerSchedule>> deviceSchedules = allocatePowerToDevices(
                optimalSchedule, devices);
        
        // 5. 计算结果汇总
        calculateResultSummary(result, optimalSchedule, deviceSchedules, 
                initialTotalSoc, devices);
        
        // 6. 验证约束
        validateConstraints(result, devices, deviceSchedules);
        
        result.setDeviceSchedules(deviceSchedules);
        result.setTotalSchedule(optimalSchedule);
        result.setOptimizationTimeMs(System.currentTimeMillis() - startOptimizationTime);
        
        return result;
    }
    
    /**
     * 动态规划求解最优调度
     * 目标: 最小化总成本，同时满足所有约束
     */
    private List<PowerSchedule> solveDynamicProgramming(
            List<DemandForecast> forecasts,
            List<StorageDevice> devices,
            double totalMaxChargePower,
            double totalMaxDischargePower,
            double totalCapacity,
            double initialSoc) {
        
        // 计算设备集群的平均SOC约束
        double avgMinSoc = devices.stream().mapToDouble(StorageDevice::getMinSoc).average().orElse(0.1);
        double avgMaxSoc = devices.stream().mapToDouble(StorageDevice::getMaxSoc).average().orElse(0.95);
        
        List<PowerSchedule> schedule = new ArrayList<>();
        double currentSoc = initialSoc;
        
        for (int i = 0; i < TIME_POINTS; i++) {
            DemandForecast forecast = forecasts.get(i);
            PowerSchedule powerSchedule = new PowerSchedule(i, forecast.getTimePoint());
            
            double electricityPrice = forecast.getPredictedPrice();
            powerSchedule.setElectricityPrice(electricityPrice);
            
            // 计算当前SOC下的可用容量（使用实际的SOC约束）
            double availableChargeCapacity = totalCapacity * (avgMaxSoc - currentSoc);
            double availableDischargeCapacity = totalCapacity * (currentSoc - avgMinSoc);
            
            // 计算15分钟内可充/放电的最大能量
            double maxChargeEnergy = Math.min(
                    totalMaxChargePower * TIME_INTERVAL_HOURS,
                    availableChargeCapacity
            );
            double maxDischargeEnergy = Math.min(
                    totalMaxDischargePower * TIME_INTERVAL_HOURS,
                    availableDischargeCapacity
            );
            
            // 基于电价和需求决定充放电策略
            double chargePower = 0;
            double dischargePower = 0;
            
            double predictedNetDemand = forecast.getPredictedNetDemand();
            
            // 策略: 低价充电，高价放电，同时满足需求
            if (electricityPrice < 0.5) {
                // 谷时电价: 优先充电
                double targetChargeEnergy = Math.min(maxChargeEnergy, 
                        totalMaxChargePower * TIME_INTERVAL_HOURS * 0.9);
                chargePower = targetChargeEnergy / TIME_INTERVAL_HOURS;
                
                // 如果有放电需求，优先使用储能放电
                if (predictedNetDemand < 0) {
                    dischargePower = Math.min(maxDischargeEnergy / TIME_INTERVAL_HOURS, 
                            Math.abs(predictedNetDemand));
                }
                
            } else if (electricityPrice > 1.0) {
                // 峰时电价: 优先放电
                double targetDischargeEnergy = Math.min(maxDischargeEnergy,
                        totalMaxDischargePower * TIME_INTERVAL_HOURS * 0.9);
                dischargePower = targetDischargeEnergy / TIME_INTERVAL_HOURS;
                
                // 如果有充电需求，在满足放电后考虑
                if (predictedNetDemand > 0 && currentSoc < 0.3) {
                    chargePower = Math.min(maxChargeEnergy / TIME_INTERVAL_HOURS,
                            predictedNetDemand * 0.3);
                }
                
            } else {
                // 平时电价: 根据需求平衡
                if (predictedNetDemand > 0) {
                    // 需要充电
                    chargePower = Math.min(maxChargeEnergy / TIME_INTERVAL_HOURS,
                            Math.min(predictedNetDemand, totalMaxChargePower * 0.7));
                } else if (predictedNetDemand < 0) {
                    // 需要放电
                    dischargePower = Math.min(maxDischargeEnergy / TIME_INTERVAL_HOURS,
                            Math.min(Math.abs(predictedNetDemand), totalMaxDischargePower * 0.7));
                }
            }
            
            // 确保满足最低放电需求
            double minDischargeDemand = forecast.getPredictedDischargeDemand() * 0.8;
            if (dischargePower < minDischargeDemand && currentSoc > avgMinSoc + 0.1) {
                dischargePower = Math.min(minDischargeDemand, 
                        maxDischargeEnergy / TIME_INTERVAL_HOURS);
            }
            
            // 应急充电机制：当SOC接近最低值且有突发充电需求时，强制充电
            double emergencyChargePower = handleEmergencyCharge(currentSoc, avgMinSoc, 
                    avgMaxSoc, totalMaxChargePower, maxChargeEnergy, forecast);
            if (emergencyChargePower > 0) {
                chargePower = Math.max(chargePower, emergencyChargePower);
            }
            
            // 更新功率和SOC
            powerSchedule.setChargePower(chargePower);
            powerSchedule.setDischargePower(dischargePower);
            powerSchedule.calculateNetPower();
            
            // 计算SOC变化
            double chargeEfficiency = 0.95;
            double dischargeEfficiency = 0.95;
            double socChange = (chargePower * chargeEfficiency - dischargePower / dischargeEfficiency) 
                    * TIME_INTERVAL_HOURS / totalCapacity;
            
            // 使用实际的SOC约束限制
            currentSoc = Math.max(avgMinSoc, Math.min(avgMaxSoc, currentSoc + socChange));
            powerSchedule.setSocChange(socChange);
            powerSchedule.setCumulativeSoc(currentSoc);
            
            // 计算成本
            powerSchedule.calculateCost();
            
            schedule.add(powerSchedule);
        }
        
        return schedule;
    }
    
    /**
     * 应急充电处理
     * 当储能设备SOC接近最低值，但出现突发充电需求时的处理逻辑
     * 
     * @param currentSoc 当前SOC
     * @param minSoc 最小SOC限制
     * @param maxSoc 最大SOC限制
     * @param maxChargePower 最大充电功率
     * @param maxChargeEnergy 最大可充电能量
     * @param forecast 需求预测
     * @return 应急充电功率
     */
    private double handleEmergencyCharge(double currentSoc, double minSoc, double maxSoc,
                                         double maxChargePower, double maxChargeEnergy,
                                         DemandForecast forecast) {
        double emergencyChargePower = 0;
        
        // 定义应急SOC阈值（当SOC低于此值时进入应急状态）
        double emergencySocThreshold = minSoc + 0.15; // 最小SOC + 15%
        
        // 定义紧急充电需求阈值
        double emergencyChargeDemandThreshold = 300; // kW
        
        // 检查是否需要应急充电
        boolean isEmergencyStatus = currentSoc <= emergencySocThreshold;
        boolean hasEmergencyChargeDemand = forecast.getPredictedChargeDemand() > emergencyChargeDemandThreshold;
        boolean isHighPricePeriod = forecast.getPredictedPrice() > 1.0;
        
        if (isEmergencyStatus && hasEmergencyChargeDemand) {
            // 场景1: SOC低且有突发充电需求 - 必须充电
            double targetSoc = minSoc + 0.25; // 目标SOC: 最小值 + 25%
            double requiredEnergy = maxSoc * (targetSoc - currentSoc);
            double emergencyPower = Math.min(requiredEnergy / TIME_INTERVAL_HOURS, maxChargePower * 0.8);
            emergencyChargePower = Math.max(emergencyChargePower, emergencyPower);
            
        } else if (isEmergencyStatus && !isHighPricePeriod) {
            // 场景2: SOC低且电价不高 - 预防性充电
            double targetSoc = minSoc + 0.20; // 目标SOC: 最小值 + 20%
            double requiredEnergy = maxSoc * (targetSoc - currentSoc);
            double preventivePower = Math.min(requiredEnergy / TIME_INTERVAL_HOURS, maxChargePower * 0.5);
            emergencyChargePower = Math.max(emergencyChargePower, preventivePower);
            
        } else if (currentSoc <= minSoc + 0.05) {
            // 场景3: SOC极低（接近最低限制）- 强制保留充电能力
            // 即使电价高，也必须保留一定的充电能力
            double mandatoryReservePower = maxChargePower * 0.3;
            emergencyChargePower = Math.max(emergencyChargePower, mandatoryReservePower);
        }
        
        // 确保不超过最大可充电能量
        if (emergencyChargePower > 0) {
            emergencyChargePower = Math.min(emergencyChargePower, maxChargeEnergy / TIME_INTERVAL_HOURS);
        }
        
        return emergencyChargePower;
    }
    
    /**
     * 将功率分配给各个设备
     */
    private Map<String, List<PowerSchedule>> allocatePowerToDevices(
            List<PowerSchedule> totalSchedule,
            List<StorageDevice> devices) {
        
        Map<String, List<PowerSchedule>> deviceSchedules = new HashMap<>();
        
        // 初始化每个设备的调度列表
        for (StorageDevice device : devices) {
            deviceSchedules.put(device.getDeviceId(), new ArrayList<>());
        }
        
        // 为每个时间点分配功率
        for (PowerSchedule totalPoint : totalSchedule) {
            double remainingChargePower = totalPoint.getChargePower();
            double remainingDischargePower = totalPoint.getDischargePower();
            
            // 按SOC排序设备: 充电时SOC低的优先，放电时SOC高的优先
            List<StorageDevice> sortedDevices = new ArrayList<>(devices);
            
            // 分配充电功率
            if (remainingChargePower > EPSILON) {
                sortedDevices.sort(Comparator.comparingDouble(StorageDevice::getCurrentSoc));
                
                for (StorageDevice device : sortedDevices) {
                    if (remainingChargePower <= EPSILON) break;
                    
                    double maxDeviceCharge = Math.min(
                            device.getMaxChargePower(),
                            device.getAvailableChargeCapacity() / TIME_INTERVAL_HOURS
                    );
                    double allocatedPower = Math.min(maxDeviceCharge, 
                            remainingChargePower / devices.size() * 1.5);
                    
                    if (allocatedPower > device.getMinChargePower()) {
                        PowerSchedule deviceSchedule = new PowerSchedule(
                                totalPoint.getTimeIndex(), totalPoint.getTimePoint());
                        deviceSchedule.setChargePower(allocatedPower);
                        deviceSchedule.setElectricityPrice(totalPoint.getElectricityPrice());
                        deviceSchedule.calculateNetPower();
                        deviceSchedule.calculateCost();
                        
                        // 更新设备SOC
                        double socChange = allocatedPower * 0.95 * TIME_INTERVAL_HOURS 
                                / device.getRatedCapacity();
                        device.setCurrentSoc(Math.min(device.getMaxSoc(), 
                                device.getCurrentSoc() + socChange));
                        deviceSchedule.setCumulativeSoc(device.getCurrentSoc());
                        
                        deviceSchedules.get(device.getDeviceId()).add(deviceSchedule);
                        remainingChargePower -= allocatedPower;
                    }
                }
            }
            
            // 分配放电功率
            if (remainingDischargePower > EPSILON) {
                sortedDevices.sort((d1, d2) -> 
                        Double.compare(d2.getCurrentSoc(), d1.getCurrentSoc()));
                
                for (StorageDevice device : sortedDevices) {
                    if (remainingDischargePower <= EPSILON) break;
                    
                    double maxDeviceDischarge = Math.min(
                            device.getMaxDischargePower(),
                            device.getAvailableDischargeCapacity() / TIME_INTERVAL_HOURS
                    );
                    double allocatedPower = Math.min(maxDeviceDischarge,
                            remainingDischargePower / devices.size() * 1.5);
                    
                    if (allocatedPower > device.getMinDischargePower()) {
                        PowerSchedule deviceSchedule = new PowerSchedule(
                                totalPoint.getTimeIndex(), totalPoint.getTimePoint());
                        deviceSchedule.setDischargePower(allocatedPower);
                        deviceSchedule.setElectricityPrice(totalPoint.getElectricityPrice());
                        deviceSchedule.calculateNetPower();
                        deviceSchedule.calculateCost();
                        
                        // 更新设备SOC
                        double socChange = allocatedPower / 0.95 * TIME_INTERVAL_HOURS 
                                / device.getRatedCapacity();
                        device.setCurrentSoc(Math.max(device.getMinSoc(), 
                                device.getCurrentSoc() - socChange));
                        deviceSchedule.setCumulativeSoc(device.getCurrentSoc());
                        
                        deviceSchedules.get(device.getDeviceId()).add(deviceSchedule);
                        remainingDischargePower -= allocatedPower;
                    }
                }
            }
            
            // 为未分配功率的设备添加空调度
            for (StorageDevice device : devices) {
                List<PowerSchedule> schedules = deviceSchedules.get(device.getDeviceId());
                if (schedules.size() <= totalPoint.getTimeIndex()) {
                    PowerSchedule emptySchedule = new PowerSchedule(
                            totalPoint.getTimeIndex(), totalPoint.getTimePoint());
                    emptySchedule.setCumulativeSoc(device.getCurrentSoc());
                    emptySchedule.setElectricityPrice(totalPoint.getElectricityPrice());
                    schedules.add(emptySchedule);
                }
            }
        }
        
        return deviceSchedules;
    }
    
    /**
     * 计算结果汇总
     */
    private void calculateResultSummary(OptimizationResult result,
                                       List<PowerSchedule> totalSchedule,
                                       Map<String, List<PowerSchedule>> deviceSchedules,
                                       double initialSoc,
                                       List<StorageDevice> devices) {
        
        double totalCost = 0;
        double totalRevenue = 0;
        double totalChargeEnergy = 0;
        double totalDischargeEnergy = 0;
        
        for (PowerSchedule schedule : totalSchedule) {
            double cost = schedule.getEstimatedCost();
            if (cost > 0) {
                totalCost += cost;
            } else {
                totalRevenue += Math.abs(cost);
            }
            
            totalChargeEnergy += schedule.getChargePower() * TIME_INTERVAL_HOURS;
            totalDischargeEnergy += schedule.getDischargePower() * TIME_INTERVAL_HOURS;
        }
        
        result.setTotalCost(totalCost);
        result.setTotalRevenue(totalRevenue);
        result.setTotalChargeEnergy(totalChargeEnergy);
        result.setTotalDischargeEnergy(totalDischargeEnergy);
        result.setInitialTotalSoc(initialSoc);
        
        if (!totalSchedule.isEmpty()) {
            result.setFinalTotalSoc(totalSchedule.get(totalSchedule.size() - 1).getCumulativeSoc());
        }
        
        result.calculateSummary();
    }
    
    /**
     * 验证约束条件
     */
    private void validateConstraints(OptimizationResult result,
                                    List<StorageDevice> devices,
                                    Map<String, List<PowerSchedule>> deviceSchedules) {
        
        List<String> violations = new ArrayList<>();
        boolean allSatisfied = true;
        
        // 检查每个设备的约束
        for (StorageDevice device : devices) {
            List<PowerSchedule> schedules = deviceSchedules.get(device.getDeviceId());
            
            for (PowerSchedule schedule : schedules) {
                // 检查最大功率约束
                if (schedule.getChargePower() > device.getMaxChargePower() + EPSILON) {
                    violations.add(String.format(
                            "设备 %s 在时间点 %d 充电功率 %.2f 超过最大值 %.2f",
                            device.getDeviceId(), schedule.getTimeIndex(),
                            schedule.getChargePower(), device.getMaxChargePower()));
                    allSatisfied = false;
                }
                
                if (schedule.getDischargePower() > device.getMaxDischargePower() + EPSILON) {
                    violations.add(String.format(
                            "设备 %s 在时间点 %d 放电功率 %.2f 超过最大值 %.2f",
                            device.getDeviceId(), schedule.getTimeIndex(),
                            schedule.getDischargePower(), device.getMaxDischargePower()));
                    allSatisfied = false;
                }
                
                // 检查最小功率约束
                if (schedule.getChargePower() > EPSILON && 
                        schedule.getChargePower() < device.getMinChargePower() - EPSILON) {
                    violations.add(String.format(
                            "设备 %s 在时间点 %d 充电功率 %.2f 低于最小值 %.2f",
                            device.getDeviceId(), schedule.getTimeIndex(),
                            schedule.getChargePower(), device.getMinChargePower()));
                    allSatisfied = false;
                }
                
                // 检查SOC约束
                if (schedule.getCumulativeSoc() > device.getMaxSoc() + EPSILON) {
                    violations.add(String.format(
                            "设备 %s 在时间点 %d SOC %.2f 超过最大值 %.2f",
                            device.getDeviceId(), schedule.getTimeIndex(),
                            schedule.getCumulativeSoc(), device.getMaxSoc()));
                    allSatisfied = false;
                }
                
                if (schedule.getCumulativeSoc() < device.getMinSoc() - EPSILON) {
                    violations.add(String.format(
                            "设备 %s 在时间点 %d SOC %.2f 低于最小值 %.2f",
                            device.getDeviceId(), schedule.getTimeIndex(),
                            schedule.getCumulativeSoc(), device.getMinSoc()));
                    allSatisfied = false;
                }
            }
        }
        
        result.setConstraintsSatisfied(allSatisfied);
        result.setConstraintViolations(violations);
    }
}
