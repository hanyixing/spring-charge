package com.example.demo.service;

import com.example.demo.model.ChargePlan;
import com.example.demo.model.ChargeRequest;
import com.example.demo.model.ElectricityPrice;
import com.example.demo.model.TimeSlotPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 充电优化服务
 * 计算最低成本的充电方案
 */
@Slf4j
@Service
public class ChargeOptimizationService {
    
    @Autowired
    private ElectricityPriceService priceService;
    
    // 每个时间点的时长（小时）= 15分钟 = 0.25小时
    private static final BigDecimal SLOT_DURATION_HOUR = new BigDecimal("0.25");
    
    /**
     * 计算最优充电方案
     * 在满足充电需求的前提下，选择电价最低的时间段进行充电
     */
    public ChargePlan calculateOptimalChargePlan(ChargeRequest request) {
        log.info("开始计算最优充电方案: 当前SOC={}, 目标SOC={}, 时间范围={}-{}",
                request.getCurrentSoc(), request.getTargetSoc(),
                request.getStartTimeSlot(), request.getEndTimeSlot());
        
        // 1. 计算需要充电的电量
        BigDecimal socDiff = request.getTargetSoc().subtract(request.getCurrentSoc());
        BigDecimal energyNeeded = request.getBatteryCapacity()
                .multiply(socDiff)
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        
        log.info("需要充电电量: {} kWh", energyNeeded);
        
        // 2. 获取时间范围内的电价信息
        List<TimeSlotPrice> allPrices = priceService.getTimeSlotPrices(
                request.getElectricityPrice() != null ? 
                request.getElectricityPrice() : priceService.getDefaultPrice());
        
        // 3. 筛选允许充电的时间点，并按电价排序
        List<TimeSlotPrice> availableSlots = new ArrayList<>();
        for (TimeSlotPrice slot : allPrices) {
            int slotIndex = slot.getTimeSlot();
            if (isWithinTimeRange(slotIndex, request.getStartTimeSlot(), request.getEndTimeSlot())) {
                availableSlots.add(slot);
            }
        }
        
        // 按电价从低到高排序
        Collections.sort(availableSlots);
        
        // 4. 贪心算法：优先在电价低的时间段充电
        List<BigDecimal> powerSlots = new ArrayList<>(Collections.nCopies(96, BigDecimal.ZERO));
        List<BigDecimal> priceSlots = new ArrayList<>(96);
        
        // 初始化价格列表
        for (int i = 0; i < 96; i++) {
            priceSlots.add(allPrices.get(i).getPrice());
        }
        
        BigDecimal remainingEnergy = energyNeeded;
        BigDecimal totalCost = BigDecimal.ZERO;
        int firstSlot = -1;
        int lastSlot = -1;
        
        for (TimeSlotPrice slot : availableSlots) {
            if (remainingEnergy.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            
            int slotIndex = slot.getTimeSlot();
            
            // 计算该时间点最多可充的电量
            BigDecimal maxEnergyInSlot = request.getMaxChargePower().multiply(SLOT_DURATION_HOUR);
            
            // 实际充电电量 = min(剩余需充电量, 该时段最大可充电量)
            BigDecimal actualEnergy = remainingEnergy.min(maxEnergyInSlot);
            
            // 计算充电功率
            BigDecimal power = actualEnergy.divide(SLOT_DURATION_HOUR, 4, RoundingMode.HALF_UP);
            
            // 记录该时间点的充电功率
            powerSlots.set(slotIndex, power);
            
            // 计算该时间点的成本
            BigDecimal slotCost = actualEnergy.multiply(slot.getPrice());
            totalCost = totalCost.add(slotCost);
            
            // 更新剩余电量
            remainingEnergy = remainingEnergy.subtract(actualEnergy);
            
            // 记录实际充电时间范围
            if (firstSlot == -1) {
                firstSlot = slotIndex;
            }
            lastSlot = slotIndex;
            
            log.debug("时间点 {}: 充电功率={}kW, 充电电量={}kWh, 电价={}元/kWh, 成本={}元",
                    priceService.timeSlotToString(slotIndex),
                    power, actualEnergy, slot.getPrice(), slotCost);
        }
        
        // 5. 构建充电计划结果
        ChargePlan plan = new ChargePlan();
        plan.setPowerSlots(powerSlots);
        plan.setPriceSlots(priceSlots);
        plan.setTotalEnergy(energyNeeded.subtract(remainingEnergy));
        plan.setTotalCost(totalCost);
        plan.setActualStartSlot(firstSlot);
        plan.setActualEndSlot(lastSlot);
        plan.setFinalSoc(request.getCurrentSoc().add(
                plan.getTotalEnergy().multiply(new BigDecimal("100"))
                        .divide(request.getBatteryCapacity(), 2, RoundingMode.HALF_UP)));
        
        // 计算充电时长
        if (firstSlot != -1 && lastSlot != -1) {
            int durationSlots = lastSlot - firstSlot + 1;
            plan.setDurationMinutes(durationSlots * 15);
        } else {
            plan.setDurationMinutes(0);
        }
        
        log.info("最优充电方案计算完成: 总充电量={}kWh, 总成本={}元, 充电时长={}分钟",
                plan.getTotalEnergy(), plan.getTotalCost(), plan.getDurationMinutes());
        
        return plan;
    }
    
    /**
     * 判断时间点是否在允许的时间范围内
     * 支持跨天的时间范围（如 22:00-06:00）
     */
    private boolean isWithinTimeRange(int slot, int startSlot, int endSlot) {
        if (startSlot <= endSlot) {
            // 不跨天的情况
            return slot >= startSlot && slot <= endSlot;
        } else {
            // 跨天的情况（如 22:00-06:00）
            return slot >= startSlot || slot <= endSlot;
        }
    }
    
    /**
     * 计算在固定功率下的充电方案（非优化模式）
     */
    public ChargePlan calculateFixedPowerPlan(ChargeRequest request, BigDecimal fixedPower) {
        log.info("开始计算固定功率充电方案: 功率={}kW", fixedPower);
        
        // 1. 计算需要充电的电量
        BigDecimal socDiff = request.getTargetSoc().subtract(request.getCurrentSoc());
        BigDecimal energyNeeded = request.getBatteryCapacity()
                .multiply(socDiff)
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        
        // 2. 获取电价信息
        List<TimeSlotPrice> allPrices = priceService.getTimeSlotPrices(
                request.getElectricityPrice() != null ? 
                request.getElectricityPrice() : priceService.getDefaultPrice());
        
        List<BigDecimal> powerSlots = new ArrayList<>(Collections.nCopies(96, BigDecimal.ZERO));
        List<BigDecimal> priceSlots = new ArrayList<>(96);
        
        for (int i = 0; i < 96; i++) {
            priceSlots.add(allPrices.get(i).getPrice());
        }
        
        BigDecimal remainingEnergy = energyNeeded;
        BigDecimal totalCost = BigDecimal.ZERO;
        int firstSlot = -1;
        int lastSlot = -1;
        
        // 按时间顺序充电
        for (int i = request.getStartTimeSlot(); ; i = (i + 1) % 96) {
            if (remainingEnergy.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            
            // 检查是否超出时间范围
            if (firstSlot != -1 && i == request.getStartTimeSlot()) {
                break; // 已经循环一圈了
            }
            
            // 计算该时间点可充电量
            BigDecimal maxEnergyInSlot = fixedPower.multiply(SLOT_DURATION_HOUR);
            BigDecimal actualEnergy = remainingEnergy.min(maxEnergyInSlot);
            
            BigDecimal power = actualEnergy.divide(SLOT_DURATION_HOUR, 4, RoundingMode.HALF_UP);
            powerSlots.set(i, power);
            
            BigDecimal slotCost = actualEnergy.multiply(allPrices.get(i).getPrice());
            totalCost = totalCost.add(slotCost);
            
            remainingEnergy = remainingEnergy.subtract(actualEnergy);
            
            if (firstSlot == -1) {
                firstSlot = i;
            }
            lastSlot = i;
            
            // 如果到了结束时间点，停止
            if (i == request.getEndTimeSlot() && remainingEnergy.compareTo(BigDecimal.ZERO) > 0) {
                log.warn("到达结束时间点但充电未完成，剩余电量: {} kWh", remainingEnergy);
                break;
            }
        }
        
        ChargePlan plan = new ChargePlan();
        plan.setPowerSlots(powerSlots);
        plan.setPriceSlots(priceSlots);
        plan.setTotalEnergy(energyNeeded.subtract(remainingEnergy));
        plan.setTotalCost(totalCost);
        plan.setActualStartSlot(firstSlot);
        plan.setActualEndSlot(lastSlot);
        plan.setFinalSoc(request.getCurrentSoc().add(
                plan.getTotalEnergy().multiply(new BigDecimal("100"))
                        .divide(request.getBatteryCapacity(), 2, RoundingMode.HALF_UP)));
        
        if (firstSlot != -1 && lastSlot != -1) {
            int durationSlots = (lastSlot >= firstSlot) ? 
                    (lastSlot - firstSlot + 1) : (96 - firstSlot + lastSlot + 1);
            plan.setDurationMinutes(durationSlots * 15);
        } else {
            plan.setDurationMinutes(0);
        }
        
        return plan;
    }
}
