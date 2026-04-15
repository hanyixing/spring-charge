package com.example.demo.controller;

import com.example.demo.model.BatterySOC;
import com.example.demo.model.ChargingCurve;
import com.example.demo.model.ChargingRecommendation;
import com.example.demo.model.ElectricityPrice;
import com.example.demo.model.UserChargingHabit;
import com.example.demo.service.ChargingRecommendationService;
import com.example.demo.service.MockDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;

/**
 * 充电推荐控制器
 */
@RestController
@RequestMapping("/api/charging")
public class ChargingRecommendationController {
    private static final Logger log = LoggerFactory.getLogger(ChargingRecommendationController.class);
    
    @Autowired
    private ChargingRecommendationService recommendationService;
    
    @Autowired
    private MockDataService mockDataService;
    
    /**
     * 获取用户充电推荐
     * 
     * @param userId 用户ID
     * @param targetEnergy 目标充电量（kWh），默认40
     * @return 充电推荐结果
     */
    @GetMapping("/recommend/{userId}")
    public Map<String, Object> getRecommendation(
            @PathVariable String userId,
            @RequestParam(defaultValue = "40") double targetEnergy) {
        
        log.info("获取用户[{}]的充电推荐，目标充电量: {} kWh", userId, targetEnergy);
        
        long startTime = System.currentTimeMillis();
        
        // 1. 获取用户历史充电数据（模拟数据）
        List<ChargingCurve> historyCurves = mockDataService.generateHistoryCurves(userId, 30);
        
        // 2. 分析用户充电习惯
        UserChargingHabit userHabit = recommendationService.analyzeUserHabit(userId, historyCurves);
        
        // 3. 获取电价信息
        ElectricityPrice price = mockDataService.getElectricityPrice(LocalDate.now());
        
        // 4. 生成充电推荐
        ChargingRecommendation recommendation = recommendationService.generateRecommendation(
                userId, userHabit, targetEnergy, price);
        
        long endTime = System.currentTimeMillis();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("userId", userId);
        response.put("targetEnergy", targetEnergy);
        response.put("recommendation", recommendation);
        response.put("userHabit", userHabit);
        response.put("processingTimeMs", endTime - startTime);
        
        return response;
    }
    
    /**
     * 获取用户充电习惯分析
     * 
     * @param userId 用户ID
     * @return 用户充电习惯
     */
    @GetMapping("/habit/{userId}")
    public Map<String, Object> getUserHabit(@PathVariable String userId) {
        log.info("获取用户[{}]的充电习惯分析", userId);
        
        List<ChargingCurve> historyCurves = mockDataService.generateHistoryCurves(userId, 30);
        UserChargingHabit habit = recommendationService.analyzeUserHabit(userId, historyCurves);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("userId", userId);
        response.put("habit", habit);
        
        return response;
    }
    
    /**
     * 获取电价信息
     * 
     * @param date 日期（yyyy-MM-dd），默认为今天
     * @return 电价信息
     */
    @GetMapping("/price")
    public Map<String, Object> getElectricityPrice(
            @RequestParam(required = false) String date) {
        
        LocalDate targetDate = date != null ? 
                LocalDate.parse(date) : LocalDate.now();
        
        ElectricityPrice price = mockDataService.getElectricityPrice(targetDate);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("date", targetDate.toString());
        response.put("price", price);
        
        return response;
    }
    
    /**
     * 对比不同充电策略
     * 
     * @param userId 用户ID
     * @param targetEnergy 目标充电量
     * @return 不同策略的对比结果
     */
    @GetMapping("/compare/{userId}")
    public Map<String, Object> compareStrategies(
            @PathVariable String userId,
            @RequestParam(defaultValue = "40") double targetEnergy) {
        
        log.info("对比用户[{}]的不同充电策略", userId);
        
        List<ChargingCurve> historyCurves = mockDataService.generateHistoryCurves(userId, 30);
        UserChargingHabit userHabit = recommendationService.analyzeUserHabit(userId, historyCurves);
        ElectricityPrice price = mockDataService.getElectricityPrice(LocalDate.now());
        
        // 智能推荐策略
        ChargingRecommendation smartRecommendation = recommendationService.generateRecommendation(
                userId, userHabit, targetEnergy, price);
        
        // 立即充电策略（马上开始）
        ChargingRecommendation immediateRecommendation = generateImmediateStrategy(
                userId, targetEnergy, price);
        
        // 纯成本优化策略（完全按电价最低）
        ChargingRecommendation costOnlyRecommendation = generateCostOnlyStrategy(
                userId, targetEnergy, price);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("userId", userId);
        response.put("targetEnergy", targetEnergy);
        
        Map<String, Object> strategies = new HashMap<>();
        strategies.put("smart", createStrategySummary("智能推荐", smartRecommendation));
        strategies.put("immediate", createStrategySummary("立即充电", immediateRecommendation));
        strategies.put("costOnly", createStrategySummary("纯成本优化", costOnlyRecommendation));
        
        response.put("strategies", strategies);
        response.put("comparison", createComparison(smartRecommendation, immediateRecommendation, costOnlyRecommendation));
        
        return response;
    }
    
    /**
     * 批量测试多个用户
     * 
     * @param userCount 测试用户数
     * @return 批量测试结果
     */
    @GetMapping("/batch-test")
    public Map<String, Object> batchTest(
            @RequestParam(defaultValue = "10") int userCount) {
        
        log.info("批量测试{}个用户的充电推荐", userCount);
        
        List<Map<String, Object>> results = new ArrayList<>();
        double totalCostSaving = 0;
        double totalPeakShaving = 0;
        
        for (int i = 1; i <= userCount; i++) {
            String userId = "USER_" + String.format("%03d", i);
            double targetEnergy = 30 + new Random().nextDouble() * 30; // 30-60 kWh
            
            List<ChargingCurve> historyCurves = mockDataService.generateHistoryCurves(userId, 30);
            UserChargingHabit habit = recommendationService.analyzeUserHabit(userId, historyCurves);
            ElectricityPrice price = mockDataService.getElectricityPrice(LocalDate.now());
            
            ChargingRecommendation recommendation = recommendationService.generateRecommendation(
                    userId, habit, targetEnergy, price);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("targetEnergy", targetEnergy);
            result.put("estimatedCost", recommendation.getEstimatedTotalCost());
            result.put("costScore", recommendation.getCostOptimizationScore());
            result.put("peakShavingScore", recommendation.getPeakShavingScore());
            result.put("habitMatchScore", recommendation.getHabitMatchScore());
            result.put("overallScore", recommendation.getOverallScore());
            
            results.add(result);
            totalCostSaving += recommendation.getCostOptimizationScore();
            totalPeakShaving += recommendation.getPeakShavingScore();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("userCount", userCount);
        response.put("results", results);
        response.put("avgCostScore", totalCostSaving / userCount);
        response.put("avgPeakShavingScore", totalPeakShaving / userCount);
        
        return response;
    }
    
    /**
     * 获取96点充电曲线可视化数据
     *
     * @param userId 用户ID
     * @param targetEnergy 目标充电量
     * @return 可视化数据
     */
    @GetMapping("/visualization/{userId}")
    public Map<String, Object> getVisualizationData(
            @PathVariable String userId,
            @RequestParam(defaultValue = "40") double targetEnergy) {

        List<ChargingCurve> historyCurves = mockDataService.generateHistoryCurves(userId, 30);
        UserChargingHabit habit = recommendationService.analyzeUserHabit(userId, historyCurves);
        ElectricityPrice price = mockDataService.getElectricityPrice(LocalDate.now());

        ChargingRecommendation recommendation = recommendationService.generateRecommendation(
                userId, habit, targetEnergy, price);

        // 生成时间标签
        List<String> timeLabels = new ArrayList<>();
        for (int i = 0; i < 96; i++) {
            int hour = i / 4;
            int minute = (i % 4) * 15;
            timeLabels.add(String.format("%02d:%02d", hour, minute));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("timeLabels", timeLabels);
        response.put("recommendedPower", recommendation.getRecommendedPower());
        response.put("electricityPrice", recommendation.getElectricityPrice());
        response.put("recommendation", recommendation);

        return response;
    }

    /**
     * 基于SOC的充电推荐
     *
     * @param userId 用户ID
     * @param currentSOC 当前SOC（%），默认20
     * @param targetSOC 目标SOC（%），默认80
     * @param batteryCapacity 电池容量（kWh），默认60
     * @param departureTime 预计离开时间（HH:mm格式），默认22:00
     * @return 充电推荐结果（含SOC信息）
     */
    @GetMapping("/recommend-soc/{userId}")
    public Map<String, Object> getRecommendationWithSOC(
            @PathVariable String userId,
            @RequestParam(defaultValue = "20") double currentSOC,
            @RequestParam(defaultValue = "80") double targetSOC,
            @RequestParam(defaultValue = "60") double batteryCapacity,
            @RequestParam(defaultValue = "22:00") String departureTime) {

        log.info("获取用户[{}]的SOC约束充电推荐，当前SOC: {}%, 目标SOC: {}%", userId, currentSOC, targetSOC);

        long startTime = System.currentTimeMillis();

        // 1. 解析离开时间
        int departureSlot = parseTimeToSlot(departureTime);

        // 2. 创建电池SOC对象
        BatterySOC batterySOC = BatterySOC.builder()
                .totalCapacity(batteryCapacity)
                .currentSOC(currentSOC)
                .targetSOC(targetSOC)
                .minAllowedSOC(10.0)
                .maxAllowedSOC(90.0)
                .chargingEfficiency(0.92)
                .expectedDepartureSlot(departureSlot)
                .build();

        // 3. 获取用户历史充电数据
        List<ChargingCurve> historyCurves = mockDataService.generateHistoryCurves(userId, 30);

        // 4. 分析用户充电习惯
        UserChargingHabit userHabit = recommendationService.analyzeUserHabit(userId, historyCurves);

        // 5. 获取电价信息
        ElectricityPrice price = mockDataService.getElectricityPrice(LocalDate.now());

        // 6. 生成SOC约束的充电推荐
        ChargingRecommendation recommendation = recommendationService.generateRecommendationWithSOC(
                userId, userHabit, batterySOC, price);

        long endTime = System.currentTimeMillis();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("userId", userId);
        
        Map<String, Object> batteryInfo = new HashMap<>();
        batteryInfo.put("currentSOC", currentSOC);
        batteryInfo.put("targetSOC", targetSOC);
        batteryInfo.put("batteryCapacity", batteryCapacity);
        batteryInfo.put("departureTime", departureTime);
        batteryInfo.put("requiredEnergy", String.format("%.2f", batterySOC.calculateRequiredEnergy()));
        response.put("batteryInfo", batteryInfo);
        
        response.put("recommendation", recommendation);
        response.put("processingTimeMs", endTime - startTime);

        return response;
    }

    /**
     * 获取SOC可视化数据
     *
     * @param userId 用户ID
     * @param currentSOC 当前SOC（%）
     * @param targetSOC 目标SOC（%）
     * @param batteryCapacity 电池容量（kWh）
     * @param departureTime 预计离开时间
     * @return 可视化数据（含SOC曲线）
     */
    @GetMapping("/visualization-soc/{userId}")
    public Map<String, Object> getSOCVisualizationData(
            @PathVariable String userId,
            @RequestParam(defaultValue = "20") double currentSOC,
            @RequestParam(defaultValue = "80") double targetSOC,
            @RequestParam(defaultValue = "60") double batteryCapacity,
            @RequestParam(defaultValue = "22:00") String departureTime) {

        // 1. 解析离开时间
        int departureSlot = parseTimeToSlot(departureTime);

        // 2. 创建电池SOC对象
        BatterySOC batterySOC = BatterySOC.builder()
                .totalCapacity(batteryCapacity)
                .currentSOC(currentSOC)
                .targetSOC(targetSOC)
                .minAllowedSOC(10.0)
                .maxAllowedSOC(90.0)
                .chargingEfficiency(0.92)
                .expectedDepartureSlot(departureSlot)
                .build();

        // 3. 获取用户历史充电数据
        List<ChargingCurve> historyCurves = mockDataService.generateHistoryCurves(userId, 30);

        // 4. 分析用户充电习惯
        UserChargingHabit habit = recommendationService.analyzeUserHabit(userId, historyCurves);

        // 5. 获取电价信息
        ElectricityPrice price = mockDataService.getElectricityPrice(LocalDate.now());

        // 6. 生成SOC约束的充电推荐
        ChargingRecommendation recommendation = recommendationService.generateRecommendationWithSOC(
                userId, habit, batterySOC, price);

        // 生成时间标签
        List<String> timeLabels = new ArrayList<>();
        for (int i = 0; i < 96; i++) {
            int hour = i / 4;
            int minute = (i % 4) * 15;
            timeLabels.add(String.format("%02d:%02d", hour, minute));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("timeLabels", timeLabels);
        response.put("recommendedPower", recommendation.getRecommendedPower());
        response.put("electricityPrice", recommendation.getElectricityPrice());
        response.put("socCurve", recommendation.getSocCurve());
        response.put("initialSOC", recommendation.getInitialSOC());
        response.put("targetSOC", recommendation.getTargetSOC());
        response.put("completionSlot", recommendation.getCompletionSlot());
        response.put("canCompleteBeforeDeparture", recommendation.isCanCompleteBeforeDeparture());
        response.put("recommendation", recommendation);

        return response;
    }

    /**
     * 将时间字符串转换为slot索引
     */
    private int parseTimeToSlot(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            return hour * 4 + minute / 15;
        } catch (Exception e) {
            return 88; // 默认22:00
        }
    }
    
    // ============ 辅助方法 ============
    
    private ChargingRecommendation generateImmediateStrategy(String userId, double targetEnergy, 
            ElectricityPrice price) {
        // 立即充电：从当前时间开始（假设当前是slot 36，即09:00）
        double[] powerData = new double[96];
        int startSlot = 36;
        double power = targetEnergy / (6 * 0.25); // 6小时充完
        power = Math.min(power, 7.0);
        
        int slotsNeeded = (int) (targetEnergy / (power * 0.25));
        for (int i = 0; i < slotsNeeded && (startSlot + i) < 96; i++) {
            powerData[startSlot + i] = power;
        }
        
        double totalCost = 0;
        for (int i = 0; i < 96; i++) {
            totalCost += powerData[i] * 0.25 * price.getPriceData()[i];
        }
        
        return ChargingRecommendation.builder()
                .recommendationId(UUID.randomUUID().toString())
                .userId(userId)
                .recommendedPower(powerData)
                .electricityPrice(price.getPriceData())
                .estimatedTotalEnergy(targetEnergy)
                .estimatedTotalCost(totalCost)
                .recommendedStartSlot(startSlot)
                .recommendedEndSlot(startSlot + slotsNeeded)
                .costOptimizationScore(30)
                .peakShavingScore(50)
                .habitMatchScore(40)
                .overallScore(40)
                .recommendationDescription("立即充电策略：从当前时间开始充电，不考虑电价优化")
                .build();
    }
    
    private ChargingRecommendation generateCostOnlyStrategy(String userId, double targetEnergy,
            ElectricityPrice price) {
        // 纯成本优化：全部在谷时段充电
        double[] powerData = new double[96];
        int[] valleySlots = price.getValleyTimeSlots();
        
        if (valleySlots.length > 0) {
            double power = targetEnergy / (valleySlots.length * 0.25);
            power = Math.min(power, 7.0);
            
            for (int slot : valleySlots) {
                powerData[slot] = power;
            }
        }
        
        double totalCost = targetEnergy * price.getValleyPrice();
        
        return ChargingRecommendation.builder()
                .recommendationId(UUID.randomUUID().toString())
                .userId(userId)
                .recommendedPower(powerData)
                .electricityPrice(price.getPriceData())
                .estimatedTotalEnergy(targetEnergy)
                .estimatedTotalCost(totalCost)
                .recommendedStartSlot(valleySlots.length > 0 ? valleySlots[0] : 0)
                .recommendedEndSlot(valleySlots.length > 0 ? valleySlots[valleySlots.length - 1] : 95)
                .costOptimizationScore(95)
                .peakShavingScore(60)
                .habitMatchScore(20)
                .overallScore(58)
                .recommendationDescription("纯成本优化策略：全部在谷时段充电，成本最低但可能不符合用户习惯")
                .build();
    }
    
    private Map<String, Object> createStrategySummary(String name, ChargingRecommendation rec) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("name", name);
        summary.put("estimatedCost", String.format("%.2f元", rec.getEstimatedTotalCost()));
        summary.put("startTime", String.format("%02d:%02d", 
                rec.getRecommendedStartSlot() / 4, (rec.getRecommendedStartSlot() % 4) * 15));
        summary.put("endTime", String.format("%02d:%02d",
                rec.getRecommendedEndSlot() / 4, (rec.getRecommendedEndSlot() % 4) * 15));
        summary.put("costScore", rec.getCostOptimizationScore());
        summary.put("peakShavingScore", rec.getPeakShavingScore());
        summary.put("habitMatchScore", rec.getHabitMatchScore());
        summary.put("overallScore", rec.getOverallScore());
        summary.put("description", rec.getRecommendationDescription());
        return summary;
    }
    
    private Map<String, Object> createComparison(ChargingRecommendation smart,
            ChargingRecommendation immediate, ChargingRecommendation costOnly) {
        Map<String, Object> comparison = new HashMap<>();
        
        double costSaving = immediate.getEstimatedTotalCost() - smart.getEstimatedTotalCost();
        double costSavingPercent = costSaving / immediate.getEstimatedTotalCost() * 100;
        
        comparison.put("costSaving", String.format("%.2f元", costSaving));
        comparison.put("costSavingPercent", String.format("%.1f%%", costSavingPercent));
        comparison.put("smartVsImmediate", smart.getOverallScore() - immediate.getCostOptimizationScore());
        comparison.put("smartVsCostOnly", smart.getOverallScore() - costOnly.getOverallScore());
        comparison.put("recommendation", "智能推荐策略在成本优化和用户习惯之间取得了最佳平衡");
        
        return comparison;
    }
}
