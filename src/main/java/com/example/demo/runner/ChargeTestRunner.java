package com.example.demo.runner;

import com.example.demo.model.ChargePlan;
import com.example.demo.model.ChargeRequest;
import com.example.demo.model.ElectricityPrice;
import com.example.demo.service.ChargeOptimizationService;
import com.example.demo.service.ElectricityPriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 充电算法测试运行器
 * 应用启动时自动执行测试
 */
@Slf4j
@Component
public class ChargeTestRunner implements CommandLineRunner {
    
    @Autowired
    private ChargeOptimizationService chargeService;
    
    @Autowired
    private ElectricityPriceService priceService;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("==============================================");
        log.info("      充电省钱算法测试开始");
        log.info("==============================================");
        
        // 测试1: 基础场景 - 夜间充电（谷时电价最低）
        testBasicScenario();
        
        // 测试2: 对比优化方案与立即充电方案
        testComparison();
        
        // 测试3: 自定义电价配置
        testCustomPrice();
        
        // 测试4: 跨天时间范围
        testCrossDayScenario();
        
        log.info("==============================================");
        log.info("      充电省钱算法测试完成");
        log.info("==============================================");
    }
    
    /**
     * 测试1: 基础场景
     * 当前电量20%，目标80%，电池60度，晚上22:00到次日08:00充电
     */
    private void testBasicScenario() {
        log.info("\n【测试1】基础场景 - 夜间充电优化");
        log.info("----------------------------------------------");
        
        ChargeRequest request = new ChargeRequest();
        request.setCurrentSoc(new BigDecimal("20"));
        request.setTargetSoc(new BigDecimal("80"));
        request.setBatteryCapacity(new BigDecimal("60"));
        request.setMaxChargePower(new BigDecimal("7"));
        request.setStartTimeSlot(88); // 22:00
        request.setEndTimeSlot(32);   // 08:00 (次日)
        
        // 计算需充电量
        BigDecimal energyNeeded = request.getBatteryCapacity()
                .multiply(request.getTargetSoc().subtract(request.getCurrentSoc()))
                .divide(new BigDecimal("100"));
        
        log.info("充电需求:");
        log.info("  - 当前电量: {}%", request.getCurrentSoc());
        log.info("  - 目标电量: {}%", request.getTargetSoc());
        log.info("  - 电池容量: {} kWh", request.getBatteryCapacity());
        log.info("  - 需充电量: {} kWh", energyNeeded);
        log.info("  - 最大充电功率: {} kW", request.getMaxChargePower());
        log.info("  - 充电时间范围: {} - {}", 
                priceService.timeSlotToString(request.getStartTimeSlot()),
                priceService.timeSlotToString(request.getEndTimeSlot()));
        
        // 打印电价配置
        ElectricityPrice price = priceService.getDefaultPrice();
        log.info("\n电价配置:");
        log.info("  - 尖时电价: {} 元/kWh", price.getSharpPrice());
        log.info("  - 峰时电价: {} 元/kWh", price.getPeakPrice());
        log.info("  - 平时电价: {} 元/kWh", price.getFlatPrice());
        log.info("  - 谷时电价: {} 元/kWh", price.getValleyPrice());
        
        // 计算最优方案
        ChargePlan plan = chargeService.calculateOptimalChargePlan(request);
        
        log.info("\n最优充电方案结果:");
        log.info("  - 实际开始时间: {}", priceService.timeSlotToString(plan.getActualStartSlot()));
        log.info("  - 实际结束时间: {}", priceService.timeSlotToString(plan.getActualEndSlot()));
        log.info("  - 充电时长: {} 分钟", plan.getDurationMinutes());
        log.info("  - 总充电量: {} kWh", plan.getTotalEnergy());
        log.info("  - 总成本: {} 元", plan.getTotalCost());
        log.info("  - 最终SOC: {}%", plan.getFinalSoc());
        
        // 打印每个时间点的充电功率
        log.info("\n充电功率分配 (仅显示非零功率的时间点):");
        List<BigDecimal> powerSlots = plan.getPowerSlots();
        List<BigDecimal> priceSlots = plan.getPriceSlots();
        
        for (int i = 0; i < 96; i++) {
            if (powerSlots.get(i).compareTo(BigDecimal.ZERO) > 0) {
                log.info("  {} - 功率: {} kW, 电价: {} 元/kWh",
                        priceService.timeSlotToString(i),
                        powerSlots.get(i),
                        priceSlots.get(i));
            }
        }
    }
    
    /**
     * 测试2: 对比优化方案与立即充电方案
     */
    private void testComparison() {
        log.info("\n\n【测试2】优化方案 vs 立即充电方案对比");
        log.info("----------------------------------------------");
        
        ChargeRequest request = new ChargeRequest();
        request.setCurrentSoc(new BigDecimal("30"));
        request.setTargetSoc(new BigDecimal("90"));
        request.setBatteryCapacity(new BigDecimal("70"));
        request.setMaxChargePower(new BigDecimal("11"));
        request.setStartTimeSlot(40);  // 10:00
        request.setEndTimeSlot(84);    // 21:00
        
        log.info("充电需求:");
        log.info("  - 当前电量: {}%", request.getCurrentSoc());
        log.info("  - 目标电量: {}%", request.getTargetSoc());
        log.info("  - 电池容量: {} kWh", request.getBatteryCapacity());
        log.info("  - 充电时间范围: {} - {}", 
                priceService.timeSlotToString(request.getStartTimeSlot()),
                priceService.timeSlotToString(request.getEndTimeSlot()));
        
        // 优化方案
        ChargePlan optimalPlan = chargeService.calculateOptimalChargePlan(request);
        
        // 立即充电方案（从startTimeSlot开始以最大功率充电）
        ChargePlan immediatePlan = chargeService.calculateFixedPowerPlan(
                request, request.getMaxChargePower());
        
        // 计算节省
        BigDecimal savings = immediatePlan.getTotalCost().subtract(optimalPlan.getTotalCost());
        BigDecimal savingsPercent = savings.multiply(new BigDecimal("100"))
                .divide(immediatePlan.getTotalCost(), 2, BigDecimal.ROUND_HALF_UP);
        
        log.info("\n优化方案:");
        log.info("  - 开始时间: {}", priceService.timeSlotToString(optimalPlan.getActualStartSlot()));
        log.info("  - 结束时间: {}", priceService.timeSlotToString(optimalPlan.getActualEndSlot()));
        log.info("  - 充电时长: {} 分钟", optimalPlan.getDurationMinutes());
        log.info("  - 总成本: {} 元", optimalPlan.getTotalCost());
        
        log.info("\n立即充电方案:");
        log.info("  - 开始时间: {}", priceService.timeSlotToString(immediatePlan.getActualStartSlot()));
        log.info("  - 结束时间: {}", priceService.timeSlotToString(immediatePlan.getActualEndSlot()));
        log.info("  - 充电时长: {} 分钟", immediatePlan.getDurationMinutes());
        log.info("  - 总成本: {} 元", immediatePlan.getTotalCost());
        
        log.info("\n节省金额: {} 元 ({}%)", savings, savingsPercent);
    }
    
    /**
     * 测试3: 自定义电价配置
     */
    private void testCustomPrice() {
        log.info("\n\n【测试3】自定义电价配置测试");
        log.info("----------------------------------------------");
        
        // 创建自定义电价
        ElectricityPrice customPrice = new ElectricityPrice(
                new BigDecimal("1.5"),  // 尖时
                new BigDecimal("1.0"),  // 峰时
                new BigDecimal("0.5"),  // 平时
                new BigDecimal("0.2")   // 谷时
        );
        
        ChargeRequest request = new ChargeRequest();
        request.setCurrentSoc(new BigDecimal("25"));
        request.setTargetSoc(new BigDecimal("85"));
        request.setBatteryCapacity(new BigDecimal("50"));
        request.setMaxChargePower(new BigDecimal("7"));
        request.setStartTimeSlot(76);  // 19:00
        request.setEndTimeSlot(32);    // 08:00
        request.setElectricityPrice(customPrice);
        
        log.info("自定义电价配置:");
        log.info("  - 尖时电价: {} 元/kWh", customPrice.getSharpPrice());
        log.info("  - 峰时电价: {} 元/kWh", customPrice.getPeakPrice());
        log.info("  - 平时电价: {} 元/kWh", customPrice.getFlatPrice());
        log.info("  - 谷时电价: {} 元/kWh", customPrice.getValleyPrice());
        
        ChargePlan plan = chargeService.calculateOptimalChargePlan(request);
        
        log.info("\n最优充电方案:");
        log.info("  - 开始时间: {}", priceService.timeSlotToString(plan.getActualStartSlot()));
        log.info("  - 结束时间: {}", priceService.timeSlotToString(plan.getActualEndSlot()));
        log.info("  - 总充电量: {} kWh", plan.getTotalEnergy());
        log.info("  - 总成本: {} 元", plan.getTotalCost());
    }
    
    /**
     * 测试4: 跨天时间范围
     */
    private void testCrossDayScenario() {
        log.info("\n\n【测试4】跨天时间范围测试");
        log.info("----------------------------------------------");
        
        ChargeRequest request = new ChargeRequest();
        request.setCurrentSoc(new BigDecimal("15"));
        request.setTargetSoc(new BigDecimal("95"));
        request.setBatteryCapacity(new BigDecimal("80"));
        request.setMaxChargePower(new BigDecimal("22"));
        request.setStartTimeSlot(80);  // 20:00
        request.setEndTimeSlot(48);    // 12:00 (次日)
        
        log.info("充电需求:");
        log.info("  - 当前电量: {}%", request.getCurrentSoc());
        log.info("  - 目标电量: {}%", request.getTargetSoc());
        log.info("  - 电池容量: {} kWh", request.getBatteryCapacity());
        log.info("  - 最大充电功率: {} kW (快充)", request.getMaxChargePower());
        log.info("  - 充电时间范围: {} - {} (跨天)", 
                priceService.timeSlotToString(request.getStartTimeSlot()),
                priceService.timeSlotToString(request.getEndTimeSlot()));
        
        ChargePlan plan = chargeService.calculateOptimalChargePlan(request);
        
        log.info("\n最优充电方案:");
        log.info("  - 开始时间: {}", priceService.timeSlotToString(plan.getActualStartSlot()));
        log.info("  - 结束时间: {}", priceService.timeSlotToString(plan.getActualEndSlot()));
        log.info("  - 充电时长: {} 分钟", plan.getDurationMinutes());
        log.info("  - 总充电量: {} kWh", plan.getTotalEnergy());
        log.info("  - 总成本: {} 元", plan.getTotalCost());
        log.info("  - 平均电价: {} 元/kWh", 
                plan.getTotalCost().divide(plan.getTotalEnergy(), 4, BigDecimal.ROUND_HALF_UP));
    }
}
