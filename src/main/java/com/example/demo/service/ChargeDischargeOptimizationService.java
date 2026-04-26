package com.example.demo.service;

import com.example.demo.model.ClusterScheduleResult;
import com.example.demo.model.EnergyStorageDevice;
import com.example.demo.model.PowerSchedule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class ChargeDischargeOptimizationService {
    
    private static final int TIME_SLOTS = 96;
    private static final double TIME_INTERVAL = 0.25;
    private static final double MIN_SOC_RATIO = 0.1;
    
    @Autowired
    private DemandForecastService demandForecastService;
    
    @Autowired
    private ElectricityPriceService electricityPriceService;
    
    public ClusterScheduleResult optimizeClusterSchedule(
            List<EnergyStorageDevice> devices,
            List<Double> historicalDemand) {
        
        log.info("开始优化储能设备集群充放电调度，设备数量: {}", devices.size());
        
        List<Double> demandForecast = demandForecastService.forecastDemand(historicalDemand, TIME_SLOTS);
        List<Double> prices = electricityPriceService.getElectricityPrices();
        
        double totalMaxPower = devices.stream().mapToDouble(EnergyStorageDevice::getMaxPower).sum();
        double totalCapacity = devices.stream().mapToDouble(EnergyStorageDevice::getMaxCapacity).sum();
        double totalInitialSoc = devices.stream().mapToDouble(EnergyStorageDevice::getCurrentSoc).sum();
        
        double totalDemand = demandForecast.stream().mapToDouble(Double::doubleValue).sum() * TIME_INTERVAL;
        log.info("总需求电量: {} kWh, 总初始SOC: {} kWh, 总容量: {} kWh", 
                String.format("%.2f", totalDemand), 
                String.format("%.2f", totalInitialSoc),
                String.format("%.2f", totalCapacity));
        
        List<PowerSchedule> deviceSchedules = new ArrayList<>();
        for (EnergyStorageDevice device : devices) {
            PowerSchedule schedule = PowerSchedule.builder()
                    .deviceId(device.getId())
                    .deviceName(device.getName())
                    .build();
            schedule.initialize(TIME_SLOTS);
            deviceSchedules.add(schedule);
        }
        
        double[] currentSocs = new double[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            currentSocs[i] = devices.get(i).getCurrentSoc();
        }
        
        for (int slot = 0; slot < TIME_SLOTS; slot++) {
            double demand = demandForecast.get(slot);
            double price = prices.get(slot);
            
            if (electricityPriceService.isValleyPeriod(slot)) {
                for (int i = 0; i < devices.size(); i++) {
                    EnergyStorageDevice device = devices.get(i);
                    PowerSchedule schedule = deviceSchedules.get(i);
                    
                    double availableCapacity = device.getMaxCapacity() - currentSocs[i];
                    if (availableCapacity > device.getMinPower() * TIME_INTERVAL) {
                        double chargePower = Math.min(device.getMaxPower(), availableCapacity / TIME_INTERVAL);
                        chargePower = Math.max(chargePower, device.getMinPower());
                        
                        schedule.getChargePower().set(slot, chargePower);
                        currentSocs[i] += chargePower * TIME_INTERVAL * device.getEfficiency();
                        currentSocs[i] = Math.min(device.getMaxCapacity(), currentSocs[i]);
                    }
                }
            }
            
            double remainingDemand = demand;
            
            List<DeviceIndex> sortedDevices = new ArrayList<>();
            for (int i = 0; i < devices.size(); i++) {
                EnergyStorageDevice device = devices.get(i);
                double minSoc = device.getMaxCapacity() * MIN_SOC_RATIO;
                double availableSoc = Math.max(0, currentSocs[i] - minSoc);
                sortedDevices.add(new DeviceIndex(i, currentSocs[i], device.getMaxPower(), availableSoc));
            }
            sortedDevices.sort(Comparator.comparingDouble(d -> -d.availableSoc));
            
            for (DeviceIndex di : sortedDevices) {
                if (remainingDemand <= 0) break;
                
                int idx = di.index;
                EnergyStorageDevice device = devices.get(idx);
                PowerSchedule schedule = deviceSchedules.get(idx);
                
                double minSoc = device.getMaxCapacity() * MIN_SOC_RATIO;
                double availableSoc = Math.max(0, currentSocs[idx] - minSoc);
                
                if (availableSoc < device.getMinPower() * TIME_INTERVAL) {
                    continue;
                }
                
                double maxDischarge = Math.min(device.getMaxPower(), availableSoc / TIME_INTERVAL);
                maxDischarge = Math.min(maxDischarge, remainingDemand);
                
                if (maxDischarge >= device.getMinPower() || (maxDischarge > 0 && remainingDemand <= device.getMinPower())) {
                    if (maxDischarge > 0) {
                        schedule.getDischargePower().set(slot, maxDischarge);
                        remainingDemand -= maxDischarge;
                        
                        currentSocs[idx] -= maxDischarge * TIME_INTERVAL / device.getEfficiency();
                        currentSocs[idx] = Math.max(minSoc, currentSocs[idx]);
                    }
                }
            }
            
            for (int i = 0; i < devices.size(); i++) {
                deviceSchedules.get(i).getSocList().set(slot, currentSocs[i]);
            }
        }
        
        List<Double> clusterPower = new ArrayList<>();
        List<Double> clusterSoc = new ArrayList<>();
        
        for (int slot = 0; slot < TIME_SLOTS; slot++) {
            double totalCharge = 0.0;
            double totalDischarge = 0.0;
            double totalSoc = 0.0;
            
            for (int i = 0; i < devices.size(); i++) {
                totalCharge += deviceSchedules.get(i).getChargePower().get(slot);
                totalDischarge += deviceSchedules.get(i).getDischargePower().get(slot);
                totalSoc += deviceSchedules.get(i).getSocList().get(slot);
            }
            
            clusterPower.add(totalDischarge - totalCharge);
            clusterSoc.add(totalSoc);
        }
        
        for (PowerSchedule schedule : deviceSchedules) {
            schedule.setTotalCost(calculateDeviceCost(schedule, prices));
        }
        
        double totalCost = calculateTotalCost(deviceSchedules, prices);
        double totalDemandMet = calculateDemandMet(clusterPower, demandForecast);
        
        boolean feasible = checkFeasibility(deviceSchedules, devices);
        
        ClusterScheduleResult result = ClusterScheduleResult.builder()
                .deviceSchedules(deviceSchedules)
                .totalCost(totalCost)
                .totalDemandMet(totalDemandMet)
                .clusterPower(clusterPower)
                .clusterSoc(clusterSoc)
                .demandList(demandForecast)
                .priceList(prices)
                .feasible(feasible)
                .message(feasible ? "调度方案可行" : "调度方案存在约束违反")
                .build();
        
        log.info("优化完成，总成本: {} 元，需求满足率: {}%", 
                String.format("%.2f", totalCost), 
                String.format("%.2f", totalDemandMet * 100));
        
        return result;
    }
    
    private double calculateTotalCost(List<PowerSchedule> schedules, List<Double> prices) {
        double totalCost = 0.0;
        
        for (PowerSchedule schedule : schedules) {
            totalCost += calculateDeviceCost(schedule, prices);
        }
        
        return totalCost;
    }
    
    private double calculateDeviceCost(PowerSchedule schedule, List<Double> prices) {
        double cost = 0.0;
        
        for (int i = 0; i < TIME_SLOTS; i++) {
            double chargePower = schedule.getChargePower().get(i);
            double dischargePower = schedule.getDischargePower().get(i);
            double price = prices.get(i);
            
            cost += chargePower * TIME_INTERVAL * price;
            cost -= dischargePower * TIME_INTERVAL * price * 0.9;
        }
        
        return cost;
    }
    
    private double calculateDemandMet(List<Double> clusterPower, List<Double> demandForecast) {
        double totalDemand = 0.0;
        double totalMet = 0.0;
        
        for (int i = 0; i < TIME_SLOTS; i++) {
            double demand = demandForecast.get(i);
            double supply = Math.max(0, clusterPower.get(i));
            
            totalDemand += demand;
            totalMet += Math.min(demand, supply);
        }
        
        return totalDemand > 0 ? totalMet / totalDemand : 1.0;
    }
    
    private boolean checkFeasibility(List<PowerSchedule> schedules, List<EnergyStorageDevice> devices) {
        for (int i = 0; i < devices.size(); i++) {
            EnergyStorageDevice device = devices.get(i);
            PowerSchedule schedule = schedules.get(i);
            
            for (int slot = 0; slot < TIME_SLOTS; slot++) {
                double chargePower = schedule.getChargePower().get(slot);
                double dischargePower = schedule.getDischargePower().get(slot);
                double soc = schedule.getSocList().get(slot);
                
                if (chargePower > device.getMaxPower() + 0.01) {
                    log.warn("设备{}在时段{}充电功率{}超过最大功率{}", 
                            device.getName(), slot, chargePower, device.getMaxPower());
                    return false;
                }
                if (dischargePower > device.getMaxPower() + 0.01) {
                    log.warn("设备{}在时段{}放电功率{}超过最大功率{}", 
                            device.getName(), slot, dischargePower, device.getMaxPower());
                    return false;
                }
                if (soc < -0.01 || soc > device.getMaxCapacity() + 0.01) {
                    log.warn("设备{}在时段{}SOC{}超出容量范围[0, {}]", 
                            device.getName(), slot, soc, device.getMaxCapacity());
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private static class DeviceIndex {
        int index;
        double soc;
        double maxPower;
        double availableSoc;
        
        DeviceIndex(int index, double soc, double maxPower, double availableSoc) {
            this.index = index;
            this.soc = soc;
            this.maxPower = maxPower;
            this.availableSoc = availableSoc;
        }
    }
}
