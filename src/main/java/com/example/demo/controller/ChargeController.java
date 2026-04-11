package com.example.demo.controller;

import com.example.demo.model.ChargePlan;
import com.example.demo.model.ChargeRequest;
import com.example.demo.model.ElectricityPrice;
import com.example.demo.service.ChargeOptimizationService;
import com.example.demo.service.ElectricityPriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 充电优化控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/charge")
public class ChargeController {
    
    @Autowired
    private ChargeOptimizationService chargeService;
    
    @Autowired
    private ElectricityPriceService priceService;
    
    /**
     * 计算最优充电方案
     */
    @PostMapping("/optimal-plan")
    public Map<String, Object> calculateOptimalPlan(@RequestBody ChargeRequest request) {
        log.info("收到最优充电方案计算请求");
        
        // 参数校验
        validateRequest(request);
        
        ChargePlan plan = chargeService.calculateOptimalChargePlan(request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", plan);
        response.put("message", "最优充电方案计算成功");
        
        return response;
    }
    
    /**
     * 计算固定功率充电方案（对比用）
     */
    @PostMapping("/fixed-plan")
    public Map<String, Object> calculateFixedPlan(@RequestBody ChargeRequest request,
                                                   @RequestParam(defaultValue = "7") BigDecimal power) {
        log.info("收到固定功率充电方案计算请求: 功率={}kW", power);
        
        validateRequest(request);
        
        ChargePlan plan = chargeService.calculateFixedPowerPlan(request, power);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", plan);
        response.put("message", "固定功率充电方案计算成功");
        
        return response;
    }
    
    /**
     * 对比优化方案与固定功率方案
     */
    @PostMapping("/compare")
    public Map<String, Object> comparePlans(@RequestBody ChargeRequest request,
                                             @RequestParam(defaultValue = "7") BigDecimal fixedPower) {
        log.info("收到方案对比请求");
        
        validateRequest(request);
        
        ChargePlan optimalPlan = chargeService.calculateOptimalChargePlan(request);
        ChargePlan fixedPlan = chargeService.calculateFixedPowerPlan(request, fixedPower);
        
        // 计算节省金额和百分比
        BigDecimal savings = fixedPlan.getTotalCost().subtract(optimalPlan.getTotalCost());
        BigDecimal savingsPercent = savings.multiply(new BigDecimal("100"))
                .divide(fixedPlan.getTotalCost(), 2, BigDecimal.ROUND_HALF_UP);
        
        Map<String, Object> comparison = new HashMap<>();
        comparison.put("optimalPlan", optimalPlan);
        comparison.put("fixedPlan", fixedPlan);
        comparison.put("savingsAmount", savings);
        comparison.put("savingsPercent", savingsPercent);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", comparison);
        response.put("message", "方案对比完成");
        
        return response;
    }
    
    /**
     * 获取电价配置
     */
    @GetMapping("/price-config")
    public Map<String, Object> getPriceConfig() {
        ElectricityPrice price = priceService.getDefaultPrice();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", price);
        response.put("message", "获取电价配置成功");
        
        return response;
    }
    
    /**
     * 更新电价配置
     */
    @PostMapping("/price-config")
    public Map<String, Object> updatePriceConfig(@RequestBody ElectricityPrice price) {
        priceService.setDefaultPrice(price);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", price);
        response.put("message", "电价配置更新成功");
        
        return response;
    }
    
    /**
     * 获取96个时间点的电价信息
     */
    @GetMapping("/time-slots")
    public Map<String, Object> getTimeSlots() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", priceService.getTimeSlotPrices(priceService.getDefaultPrice()));
        response.put("message", "获取时间点电价信息成功");
        
        return response;
    }
    
    /**
     * 参数校验
     */
    private void validateRequest(ChargeRequest request) {
        if (request.getCurrentSoc() == null || 
            request.getCurrentSoc().compareTo(BigDecimal.ZERO) < 0 ||
            request.getCurrentSoc().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("当前电量百分比必须在0-100之间");
        }
        
        if (request.getTargetSoc() == null || 
            request.getTargetSoc().compareTo(BigDecimal.ZERO) < 0 ||
            request.getTargetSoc().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("目标电量百分比必须在0-100之间");
        }
        
        if (request.getTargetSoc().compareTo(request.getCurrentSoc()) <= 0) {
            throw new IllegalArgumentException("目标电量必须大于当前电量");
        }
        
        if (request.getStartTimeSlot() == null || request.getStartTimeSlot() < 0 || request.getStartTimeSlot() > 95) {
            throw new IllegalArgumentException("开始时间点必须在0-95之间");
        }
        
        if (request.getEndTimeSlot() == null || request.getEndTimeSlot() < 0 || request.getEndTimeSlot() > 95) {
            throw new IllegalArgumentException("结束时间点必须在0-95之间");
        }
        
        if (request.getBatteryCapacity() == null || request.getBatteryCapacity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("电池容量必须大于0");
        }
        
        if (request.getMaxChargePower() == null || request.getMaxChargePower().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("最大充电功率必须大于0");
        }
    }
}
