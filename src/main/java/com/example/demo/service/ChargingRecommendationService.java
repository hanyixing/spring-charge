package com.example.demo.service;

import com.example.demo.model.BatterySOC;
import com.example.demo.model.ChargingCurve;
import com.example.demo.model.ChargingRecommendation;
import com.example.demo.model.ElectricityPrice;
import com.example.demo.model.UserChargingHabit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 充电推荐算法服务
 * 
 * 算法核心思想：
 * 1. 基于用户历史充电曲线，提取用户充电习惯特征
 * 2. 结合分时电价信息，优化充电成本
 * 3. 考虑削峰平谷，平滑充电功率曲线
 * 4. 在满足用户需求的前提下，生成最优充电计划
 */
@Service
public class ChargingRecommendationService {
    private static final Logger log = LoggerFactory.getLogger(ChargingRecommendationService.class);
    
    // 算法参数
    private static final int TIME_SLOTS = 96; // 每天96个15分钟点
    private static final double SLOT_DURATION = 0.25; // 每个时段0.25小时
    private static final double MAX_CHARGING_POWER = 7.0; // 最大充电功率7kW（家用充电桩）
    private static final double MIN_CHARGING_POWER = 0.5; // 最小充电功率0.5kW
    
    // 权重参数（用于多目标优化）
    private static final double W_COST = 0.4;      // 成本权重
    private static final double W_PEAK_SHAVING = 0.3; // 削峰平谷权重
    private static final double W_HABIT = 0.3;     // 习惯匹配权重
    
    /**
     * 分析用户充电习惯
     * 
     * @param userId 用户ID
     * @param historyCurves 历史充电曲线列表
     * @return 用户充电习惯
     */
    public UserChargingHabit analyzeUserHabit(String userId, List<ChargingCurve> historyCurves) {
        log.info("分析用户[{}]的充电习惯，历史记录数: {}", userId, historyCurves.size());
        
        if (historyCurves == null || historyCurves.isEmpty()) {
            return createDefaultHabit(userId);
        }
        
        // 计算基本统计信息
        double totalEnergy = 0;
        double totalDuration = 0;
        double totalPower = 0;
        double maxPower = 0;
        double[] timeSlotProb = new double[TIME_SLOTS];
        double[] avgPowerTemplate = new double[TIME_SLOTS];
        
        for (ChargingCurve curve : historyCurves) {
            totalEnergy += curve.getTotalEnergy();
            totalDuration += curve.getChargingDuration();
            totalPower += curve.getAvgPower();
            maxPower = Math.max(maxPower, curve.getMaxPower());
            
            double[] powerData = curve.getPowerData();
            for (int i = 0; i < TIME_SLOTS; i++) {
                if (powerData[i] > 0) {
                    timeSlotProb[i] += 1;
                    avgPowerTemplate[i] += powerData[i];
                }
            }
        }
        
        int count = historyCurves.size();
        double avgEnergy = totalEnergy / count;
        double avgDuration = totalDuration / count;
        double avgPower = totalPower / count;
        
        // 归一化时间概率和功率模板
        for (int i = 0; i < TIME_SLOTS; i++) {
            timeSlotProb[i] = timeSlotProb[i] / count;
            if (timeSlotProb[i] > 0) {
                avgPowerTemplate[i] = avgPowerTemplate[i] / (count * timeSlotProb[i]);
            }
        }
        
        // 归一化功率模板
        double maxTemplatePower = Arrays.stream(avgPowerTemplate).max().orElse(1.0);
        if (maxTemplatePower > 0) {
            for (int i = 0; i < TIME_SLOTS; i++) {
                avgPowerTemplate[i] = avgPowerTemplate[i] / maxTemplatePower;
            }
        }
        
        // 识别首选充电时间段（概率>0.3的连续时段）
        List<UserChargingHabit.TimeRange> preferredRanges = extractPreferredTimeRanges(timeSlotProb);
        
        // 计算习惯稳定性评分
        double stabilityScore = calculateStabilityScore(historyCurves);
        
        return UserChargingHabit.builder()
                .userId(userId)
                .totalChargingCount(count)
                .avgChargingEnergy(avgEnergy)
                .avgChargingDuration(avgDuration)
                .avgChargingPower(avgPower)
                .maxChargingPower(maxPower)
                .timeSlotProbability(timeSlotProb)
                .avgPowerTemplate(avgPowerTemplate)
                .preferredStartTimeRanges(preferredRanges)
                .chargingFrequency(count / 30.0) // 假设数据是30天的
                .habitStabilityScore(stabilityScore)
                .build();
    }
    
    /**
     * 生成充电推荐（基于目标能量）
     * 
     * @param userId 用户ID
     * @param userHabit 用户充电习惯
     * @param targetEnergy 目标充电量（kWh）
     * @param price 电价信息
     * @return 充电推荐结果
     */
    public ChargingRecommendation generateRecommendation(
            String userId, 
            UserChargingHabit userHabit, 
            double targetEnergy,
            ElectricityPrice price) {
        
        log.info("为用户[{}]生成充电推荐，目标充电量: {} kWh", userId, targetEnergy);
        
        // 1. 基于习惯预测充电曲线
        double[] predictedCurve = predictChargingCurve(userHabit, targetEnergy);
        
        // 2. 成本优化：将充电时段向低谷电价转移
        double[] costOptimizedCurve = optimizeForCost(predictedCurve, price, targetEnergy);
        
        // 3. 削峰平谷优化：平滑功率曲线
        double[] finalCurve = optimizeForPeakShaving(costOptimizedCurve, price);
        
        // 4. 计算各项指标
        double totalCost = calculateTotalCost(finalCurve, price);
        double actualEnergy = Arrays.stream(finalCurve).sum() * SLOT_DURATION;
        
        int startSlot = findStartSlot(finalCurve);
        int endSlot = findEndSlot(finalCurve);
        
        double costScore = calculateCostScore(finalCurve, price, targetEnergy);
        double peakShavingScore = calculatePeakShavingScore(finalCurve, price);
        double habitScore = calculateHabitMatchScore(finalCurve, userHabit);
        double overallScore = W_COST * costScore + W_PEAK_SHAVING * peakShavingScore + W_HABIT * habitScore;
        
        // 生成推荐说明
        String description = generateRecommendationDescription(
                startSlot, endSlot, actualEnergy, totalCost, costScore, peakShavingScore);
        
        return ChargingRecommendation.builder()
                .recommendationId(UUID.randomUUID().toString())
                .userId(userId)
                .generateTime(LocalDateTime.now())
                .targetDate(price.getDate().toString())
                .recommendedPower(finalCurve)
                .electricityPrice(price.getPriceData())
                .estimatedTotalEnergy(actualEnergy)
                .estimatedTotalCost(totalCost)
                .recommendedStartSlot(startSlot)
                .recommendedEndSlot(endSlot)
                .peakShavingScore(peakShavingScore)
                .costOptimizationScore(costScore)
                .habitMatchScore(habitScore)
                .overallScore(overallScore)
                .recommendationDescription(description)
                .build();
    }
    
    /**
     * 生成充电推荐（基于SOC约束）
     * 
     * @param userId 用户ID
     * @param userHabit 用户充电习惯
     * @param batterySOC 电池SOC信息
     * @param price 电价信息
     * @return 充电推荐结果
     */
    public ChargingRecommendation generateRecommendationWithSOC(
            String userId, 
            UserChargingHabit userHabit, 
            BatterySOC batterySOC,
            ElectricityPrice price) {
        
        double targetEnergy = batterySOC.calculateRequiredEnergy();
        log.info("为用户[{}]生成SOC约束充电推荐，当前SOC: {}%, 目标SOC: {}%, 需充电: {} kWh", 
                userId, batterySOC.getCurrentSOC(), batterySOC.getTargetSOC(), targetEnergy);
        
        // 1. 基于SOC约束和习惯预测充电曲线
        double[] predictedCurve = predictChargingCurveWithSOC(userHabit, batterySOC);
        
        // 2. 成本优化：将充电时段向低谷电价转移（考虑SOC约束）
        double[] costOptimizedCurve = optimizeForCostWithSOC(predictedCurve, price, batterySOC);
        
        // 3. 削峰平谷优化：平滑功率曲线
        double[] finalCurve = optimizeForPeakShaving(costOptimizedCurve, price);
        
        // 4. 计算SOC曲线
        double[] socCurve = calculateSOCCurve(finalCurve, batterySOC);
        
        // 5. 计算各项指标
        double totalCost = calculateTotalCost(finalCurve, price);
        double actualEnergy = Arrays.stream(finalCurve).sum() * SLOT_DURATION;
        double actualChargedEnergy = actualEnergy * batterySOC.getChargingEfficiency();
        double finalSOC = batterySOC.calculateNewSOC(actualChargedEnergy);
        
        int startSlot = findStartSlot(finalCurve);
        int endSlot = findEndSlot(finalCurve);
        int completionSlot = findCompletionSlot(socCurve, batterySOC.getTargetSOC());
        
        double costScore = calculateCostScore(finalCurve, price, targetEnergy);
        double peakShavingScore = calculatePeakShavingScore(finalCurve, price);
        double habitScore = calculateHabitMatchScore(finalCurve, userHabit);
        double socAchievementRate = Math.min(100, (finalSOC / batterySOC.getTargetSOC()) * 100);
        boolean canComplete = batterySOC.hasTimeConstraint() ? 
                completionSlot <= batterySOC.getExpectedDepartureSlot() : true;
        
        double overallScore = W_COST * costScore + W_PEAK_SHAVING * peakShavingScore + W_HABIT * habitScore;
        
        // 生成推荐说明
        String description = generateSOCRecommendationDescription(
                startSlot, endSlot, completionSlot, batterySOC, finalSOC, 
                actualEnergy, totalCost, canComplete, costScore);
        
        return ChargingRecommendation.builder()
                .recommendationId(UUID.randomUUID().toString())
                .userId(userId)
                .generateTime(LocalDateTime.now())
                .targetDate(price.getDate().toString())
                .recommendedPower(finalCurve)
                .electricityPrice(price.getPriceData())
                .estimatedTotalEnergy(actualEnergy)
                .estimatedTotalCost(totalCost)
                .recommendedStartSlot(startSlot)
                .recommendedEndSlot(endSlot)
                .peakShavingScore(peakShavingScore)
                .costOptimizationScore(costScore)
                .habitMatchScore(habitScore)
                .overallScore(overallScore)
                .recommendationDescription(description)
                // SOC相关字段
                .initialSOC(batterySOC.getCurrentSOC())
                .targetSOC(batterySOC.getTargetSOC())
                .socCurve(socCurve)
                .batteryCapacity(batterySOC.getTotalCapacity())
                .expectedDepartureSlot(batterySOC.getExpectedDepartureSlot())
                .socAchievementRate(socAchievementRate)
                .canCompleteBeforeDeparture(canComplete)
                .completionSlot(completionSlot)
                .build();
    }
    
    /**
     * 基于历史习惯预测充电曲线
     */
    private double[] predictChargingCurve(UserChargingHabit habit, double targetEnergy) {
        double[] curve = new double[TIME_SLOTS];
        double[] template = habit.getAvgPowerTemplate();
        double[] prob = habit.getTimeSlotProbability();
        
        // 如果没有历史模板，使用默认的充电模式
        if (template == null || Arrays.stream(template).sum() == 0) {
            return createDefaultChargingCurve(targetEnergy);
        }
        
        // 根据概率和模板生成初始曲线
        for (int i = 0; i < TIME_SLOTS; i++) {
            curve[i] = template[i] * prob[i] * habit.getMaxChargingPower();
        }
        
        // 调整曲线以满足目标充电量
        double currentEnergy = Arrays.stream(curve).sum() * SLOT_DURATION;
        if (currentEnergy > 0) {
            double scale = targetEnergy / currentEnergy;
            for (int i = 0; i < TIME_SLOTS; i++) {
                curve[i] = Math.min(curve[i] * scale, MAX_CHARGING_POWER);
                curve[i] = Math.max(curve[i], 0);
            }
        }
        
        return curve;
    }
    
    /**
     * 成本优化：将充电时段向低谷电价转移
     */
    private double[] optimizeForCost(double[] curve, ElectricityPrice price, double targetEnergy) {
        double[] optimized = Arrays.copyOf(curve, TIME_SLOTS);
        double[] prices = price.getPriceData();
        
        // 获取谷时段和峰时段
        int[] valleySlots = price.getValleyTimeSlots();
        int[] peakSlots = price.getPeakTimeSlots();
        
        // 计算当前总能量
        double currentEnergy = Arrays.stream(optimized).sum() * SLOT_DURATION;
        double remainingEnergy = Math.max(0, targetEnergy - currentEnergy);
        
        // 优先在谷时段增加充电功率
        if (remainingEnergy > 0 && valleySlots.length > 0) {
            double energyPerSlot = remainingEnergy / valleySlots.length;
            double powerPerSlot = energyPerSlot / SLOT_DURATION;
            
            for (int slot : valleySlots) {
                optimized[slot] = Math.min(optimized[slot] + powerPerSlot, MAX_CHARGING_POWER);
            }
        }
        
        // 减少峰时段充电功率
        double peakReduction = 0.3; // 减少30%
        for (int slot : peakSlots) {
            optimized[slot] = optimized[slot] * (1 - peakReduction);
        }
        
        // 重新平衡以满足目标充电量
        double newEnergy = Arrays.stream(optimized).sum() * SLOT_DURATION;
        if (newEnergy < targetEnergy && newEnergy > 0) {
            double scale = targetEnergy / newEnergy;
            for (int i = 0; i < TIME_SLOTS; i++) {
                optimized[i] = Math.min(optimized[i] * scale, MAX_CHARGING_POWER);
            }
        }
        
        return optimized;
    }
    
    /**
     * 削峰平谷优化：平滑功率曲线
     */
    private double[] optimizeForPeakShaving(double[] curve, ElectricityPrice price) {
        double[] optimized = Arrays.copyOf(curve, TIME_SLOTS);
        
        // 使用移动平均平滑曲线
        int windowSize = 3;
        double[] smoothed = new double[TIME_SLOTS];
        
        for (int i = 0; i < TIME_SLOTS; i++) {
            double sum = 0;
            int count = 0;
            for (int j = -windowSize/2; j <= windowSize/2; j++) {
                int idx = i + j;
                if (idx >= 0 && idx < TIME_SLOTS) {
                    sum += optimized[idx];
                    count++;
                }
            }
            smoothed[i] = count > 0 ? sum / count : 0;
        }
        
        // 保持总能量不变
        double originalEnergy = Arrays.stream(optimized).sum();
        double smoothedEnergy = Arrays.stream(smoothed).sum();
        
        if (smoothedEnergy > 0) {
            double scale = originalEnergy / smoothedEnergy;
            for (int i = 0; i < TIME_SLOTS; i++) {
                optimized[i] = Math.min(smoothed[i] * scale, MAX_CHARGING_POWER);
                optimized[i] = Math.max(optimized[i], 0);
            }
        }
        
        return optimized;
    }
    
    /**
     * 计算总成本
     */
    private double calculateTotalCost(double[] curve, ElectricityPrice price) {
        double[] prices = price.getPriceData();
        double totalCost = 0;
        for (int i = 0; i < TIME_SLOTS; i++) {
            totalCost += curve[i] * SLOT_DURATION * prices[i];
        }
        return totalCost;
    }
    
    /**
     * 计算成本优化评分
     */
    private double calculateCostScore(double[] curve, ElectricityPrice price, double targetEnergy) {
        double actualCost = calculateTotalCost(curve, price);
        
        // 计算理论最低成本（全部在谷时段充电）
        double minCost = targetEnergy * price.getValleyPrice();
        
        // 计算理论最高成本（全部在峰时段充电）
        double maxCost = targetEnergy * price.getPeakPrice();
        
        if (maxCost <= minCost) return 100;
        
        // 成本越低，评分越高
        double score = 100 * (maxCost - actualCost) / (maxCost - minCost);
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * 计算削峰平谷评分
     */
    private double calculatePeakShavingScore(double[] curve, ElectricityPrice price) {
        double avgPower = Arrays.stream(curve).average().orElse(0);
        if (avgPower == 0) return 0;
        
        // 计算标准差，越小表示越平滑
        double variance = 0;
        for (double power : curve) {
            variance += Math.pow(power - avgPower, 2);
        }
        double stdDev = Math.sqrt(variance / TIME_SLOTS);
        
        // 变异系数（标准差/平均值）
        double cv = stdDev / avgPower;
        
        // CV越小，评分越高
        double score = Math.max(0, 100 - cv * 100);
        return score;
    }
    
    /**
     * 计算习惯匹配度评分
     */
    private double calculateHabitMatchScore(double[] curve, UserChargingHabit habit) {
        double[] prob = habit.getTimeSlotProbability();
        if (prob == null || Arrays.stream(prob).sum() == 0) {
            return 50; // 默认中等评分
        }
        
        double score = 0;
        double totalWeight = 0;
        
        for (int i = 0; i < TIME_SLOTS; i++) {
            if (curve[i] > 0) {
                score += prob[i] * 100;
                totalWeight += 1;
            }
        }
        
        return totalWeight > 0 ? score / totalWeight : 0;
    }
    
    /**
     * 提取首选充电时间段
     */
    private List<UserChargingHabit.TimeRange> extractPreferredTimeRanges(double[] timeSlotProb) {
        List<UserChargingHabit.TimeRange> ranges = new ArrayList<>();
        
        int start = -1;
        for (int i = 0; i < TIME_SLOTS; i++) {
            if (timeSlotProb[i] > 0.3 && start == -1) {
                start = i;
            } else if ((timeSlotProb[i] <= 0.3 || i == TIME_SLOTS - 1) && start != -1) {
                int end = (timeSlotProb[i] > 0.3) ? i : i - 1;
                double prob = 0;
                for (int j = start; j <= end; j++) {
                    prob += timeSlotProb[j];
                }
                prob /= (end - start + 1);
                
                ranges.add(UserChargingHabit.TimeRange.builder()
                        .startSlot(start)
                        .endSlot(end)
                        .probability(prob)
                        .description(String.format("%02d:%02d-%02d:%02d", 
                                start/4, (start%4)*15, end/4, (end%4)*15))
                        .build());
                start = -1;
            }
        }
        
        return ranges;
    }
    
    /**
     * 计算习惯稳定性评分
     */
    private double calculateStabilityScore(List<ChargingCurve> curves) {
        if (curves.size() < 2) return 50;
        
        // 计算充电开始时间的标准差
        double[] startTimes = curves.stream()
                .mapToDouble(ChargingCurve::getStartTimeSlot)
                .toArray();
        
        double avgStart = Arrays.stream(startTimes).average().orElse(0);
        double variance = Arrays.stream(startTimes)
                .map(t -> Math.pow(t - avgStart, 2))
                .average()
                .orElse(0);
        double stdDev = Math.sqrt(variance);
        
        // 标准差越小，稳定性越高
        return Math.max(0, 100 - stdDev * 2);
    }
    
    /**
     * 创建默认充电习惯（新用户）
     */
    private UserChargingHabit createDefaultHabit(String userId) {
        double[] defaultProb = new double[TIME_SLOTS];
        double[] defaultTemplate = new double[TIME_SLOTS];
        
        // 默认晚上22:00-06:00充电概率较高
        for (int i = 88; i < 96; i++) defaultProb[i] = 0.5;
        for (int i = 0; i < 24; i++) defaultProb[i] = 0.5;
        
        Arrays.fill(defaultTemplate, 0.5);
        
        List<UserChargingHabit.TimeRange> defaultRanges = new ArrayList<>();
        defaultRanges.add(UserChargingHabit.TimeRange.builder()
                .startSlot(88)
                .endSlot(23)
                .probability(0.5)
                .description("22:00-06:00")
                .build());
        
        return UserChargingHabit.builder()
                .userId(userId)
                .totalChargingCount(0)
                .avgChargingEnergy(30.0)
                .avgChargingDuration(6.0)
                .avgChargingPower(5.0)
                .maxChargingPower(MAX_CHARGING_POWER)
                .timeSlotProbability(defaultProb)
                .avgPowerTemplate(defaultTemplate)
                .preferredStartTimeRanges(defaultRanges)
                .chargingFrequency(0.5)
                .habitStabilityScore(50)
                .build();
    }
    
    /**
     * 创建默认充电曲线
     */
    private double[] createDefaultChargingCurve(double targetEnergy) {
        double[] curve = new double[TIME_SLOTS];
        
        // 默认晚上22:00开始充电，持续6小时（到04:00）
        int startSlot = 88; // 22:00
        int durationSlots = (int) (targetEnergy / (MAX_CHARGING_POWER * SLOT_DURATION));
        durationSlots = Math.min(durationSlots, 32); // 最多8小时
        
        double power = targetEnergy / (durationSlots * SLOT_DURATION);
        power = Math.min(power, MAX_CHARGING_POWER);
        
        for (int i = 0; i < durationSlots && (startSlot + i) < TIME_SLOTS; i++) {
            int slot = (startSlot + i) % TIME_SLOTS;
            curve[slot] = power;
        }
        
        return curve;
    }
    
    /**
     * 查找充电开始时间点
     */
    private int findStartSlot(double[] curve) {
        for (int i = 0; i < TIME_SLOTS; i++) {
            if (curve[i] > MIN_CHARGING_POWER) {
                return i;
            }
        }
        return 0;
    }
    
    /**
     * 查找充电结束时间点
     */
    private int findEndSlot(double[] curve) {
        for (int i = TIME_SLOTS - 1; i >= 0; i--) {
            if (curve[i] > MIN_CHARGING_POWER) {
                return i;
            }
        }
        return TIME_SLOTS - 1;
    }
    
    /**
     * 生成推荐说明
     */
    private String generateRecommendationDescription(int startSlot, int endSlot, 
            double energy, double cost, double costScore, double peakScore) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("建议在 %02d:%02d 开始充电，预计 %02d:%02d 完成。",
                startSlot / 4, (startSlot % 4) * 15,
                endSlot / 4, (endSlot % 4) * 15));
        sb.append(String.format("预计充电 %.1f kWh，总成本 %.2f 元。", energy, cost));
        
        if (costScore > 80) {
            sb.append("此方案成本优化效果优秀，充分利用了低谷电价时段。");
        } else if (costScore > 60) {
            sb.append("此方案成本优化效果良好。");
        }
        
        if (peakScore > 80) {
            sb.append("充电曲线平滑，有助于电网削峰平谷。");
        }
        
        return sb.toString();
    }
    
    // ========== SOC相关方法 ==========
    
    /**
     * 基于SOC约束预测充电曲线
     */
    private double[] predictChargingCurveWithSOC(UserChargingHabit habit, BatterySOC batterySOC) {
        double targetEnergy = batterySOC.calculateRequiredEnergy();
        double[] curve = new double[TIME_SLOTS];
        
        // 如果有离开时间约束，需要确保在该时间前完成充电
        int latestStartSlot = 0;
        int mustCompleteBySlot = TIME_SLOTS - 1;
        
        if (batterySOC.hasTimeConstraint()) {
            mustCompleteBySlot = batterySOC.getExpectedDepartureSlot();
            latestStartSlot = batterySOC.calculateLatestStartSlot(MAX_CHARGING_POWER);
            log.debug("SOC约束：必须在slot {}前完成，最晚开始时间slot {}", mustCompleteBySlot, latestStartSlot);
        }
        
        // 获取用户习惯的时间段
        double[] prob = habit.getTimeSlotProbability();
        double[] template = habit.getAvgPowerTemplate();
        
        // 在允许的时间范围内充电
        int availableSlots = mustCompleteBySlot - latestStartSlot + 1;
        if (availableSlots <= 0) {
            availableSlots = TIME_SLOTS;
            latestStartSlot = 0;
        }
        
        // 计算需要的功率
        double requiredPower = targetEnergy / (availableSlots * SLOT_DURATION);
        requiredPower = Math.min(requiredPower, MAX_CHARGING_POWER);
        requiredPower = Math.max(requiredPower, MIN_CHARGING_POWER);
        
        // 根据用户习惯和SOC约束分配功率
        for (int i = 0; i < availableSlots; i++) {
            int slot = (latestStartSlot + i) % TIME_SLOTS;
            double habitFactor = (prob != null && prob[slot] > 0) ? prob[slot] : 0.3;
            double templateFactor = (template != null && template[slot] > 0) ? template[slot] : 0.5;
            
            curve[slot] = requiredPower * habitFactor * templateFactor;
            curve[slot] = Math.min(curve[slot], MAX_CHARGING_POWER);
        }
        
        // 调整以满足目标能量
        double currentEnergy = Arrays.stream(curve).sum() * SLOT_DURATION;
        if (currentEnergy > 0) {
            double scale = targetEnergy / currentEnergy;
            for (int i = 0; i < TIME_SLOTS; i++) {
                if (curve[i] > 0) {
                    curve[i] = Math.min(curve[i] * scale, MAX_CHARGING_POWER);
                }
            }
        }
        
        return curve;
    }
    
    /**
     * 带SOC约束的成本优化
     */
    private double[] optimizeForCostWithSOC(double[] curve, ElectricityPrice price, BatterySOC batterySOC) {
        double[] optimized = Arrays.copyOf(curve, TIME_SLOTS);
        double targetEnergy = batterySOC.calculateRequiredEnergy();
        
        // 获取时间约束
        int mustCompleteBySlot = batterySOC.hasTimeConstraint() ? 
                batterySOC.getExpectedDepartureSlot() : TIME_SLOTS - 1;
        
        // 获取谷时段（在离开时间之前的）
        int[] valleySlots = price.getValleyTimeSlots();
        int[] peakSlots = price.getPeakTimeSlots();
        
        // 优先在离开时间前的谷时段充电
        double valleyEnergy = 0;
        for (int slot : valleySlots) {
            if (slot <= mustCompleteBySlot && optimized[slot] > 0) {
                valleyEnergy += optimized[slot] * SLOT_DURATION;
            }
        }
        
        // 如果谷时段能量不足，增加谷时段功率
        if (valleyEnergy < targetEnergy * 0.6) { // 目标：60%能量在谷时段
            double neededEnergy = targetEnergy * 0.6 - valleyEnergy;
            int availableValleySlots = 0;
            for (int slot : valleySlots) {
                if (slot <= mustCompleteBySlot) availableValleySlots++;
            }
            
            if (availableValleySlots > 0) {
                double additionalPower = neededEnergy / (availableValleySlots * SLOT_DURATION);
                for (int slot : valleySlots) {
                    if (slot <= mustCompleteBySlot) {
                        optimized[slot] = Math.min(optimized[slot] + additionalPower, MAX_CHARGING_POWER);
                    }
                }
            }
        }
        
        // 减少峰时段功率（在离开时间前）
        for (int slot : peakSlots) {
            if (slot <= mustCompleteBySlot && optimized[slot] > 0) {
                optimized[slot] = optimized[slot] * 0.7; // 减少30%
            }
        }
        
        // 重新平衡能量
        double currentEnergy = Arrays.stream(optimized).sum() * SLOT_DURATION;
        if (currentEnergy < targetEnergy && currentEnergy > 0) {
            double scale = targetEnergy / currentEnergy;
            for (int i = 0; i <= mustCompleteBySlot; i++) {
                if (optimized[i] > 0) {
                    optimized[i] = Math.min(optimized[i] * scale, MAX_CHARGING_POWER);
                }
            }
        }
        
        return optimized;
    }
    
    /**
     * 计算SOC曲线
     */
    private double[] calculateSOCCurve(double[] powerCurve, BatterySOC batterySOC) {
        double[] socCurve = new double[TIME_SLOTS];
        double currentSOC = batterySOC.getCurrentSOC();
        double totalCapacity = batterySOC.getTotalCapacity();
        double efficiency = batterySOC.getChargingEfficiency();
        
        socCurve[0] = currentSOC;
        
        for (int i = 1; i < TIME_SLOTS; i++) {
            double power = powerCurve[i - 1]; // 上一个时段的功率
            double energy = power * SLOT_DURATION * efficiency; // 实际充入电池的能量
            double socChange = (energy / totalCapacity) * 100.0;
            
            currentSOC = Math.min(currentSOC + socChange, batterySOC.getMaxAllowedSOC());
            socCurve[i] = currentSOC;
        }
        
        return socCurve;
    }
    
    /**
     * 查找SOC达到目标的时间点
     */
    private int findCompletionSlot(double[] socCurve, double targetSOC) {
        for (int i = 0; i < TIME_SLOTS; i++) {
            if (socCurve[i] >= targetSOC) {
                return i;
            }
        }
        return TIME_SLOTS - 1;
    }
    
    /**
     * 生成SOC约束的推荐说明
     */
    private String generateSOCRecommendationDescription(
            int startSlot, int endSlot, int completionSlot,
            BatterySOC batterySOC, double finalSOC,
            double energy, double cost, boolean canComplete, double costScore) {
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("电池当前SOC: %.0f%%，目标SOC: %.0f%%，", 
                batterySOC.getCurrentSOC(), batterySOC.getTargetSOC()));
        sb.append(String.format("建议 %02d:%02d 开始充电，", 
                startSlot / 4, (startSlot % 4) * 15));
        
        if (batterySOC.hasTimeConstraint()) {
            sb.append(String.format("预计 %02d:%02d 达到目标SOC，", 
                    completionSlot / 4, (completionSlot % 4) * 15));
            sb.append(String.format("预计离开时间 %02d:%02d。", 
                    batterySOC.getExpectedDepartureSlot() / 4, 
                    (batterySOC.getExpectedDepartureSlot() % 4) * 15));
            
            if (canComplete) {
                sb.append("✓ 可以在离开前完成充电。");
            } else {
                sb.append("⚠ 可能无法在离开前达到目标SOC，建议提前充电或降低目标SOC。");
            }
        } else {
            sb.append(String.format("预计 %02d:%02d 完成充电。", 
                    completionSlot / 4, (completionSlot % 4) * 15));
        }
        
        sb.append(String.format("预计充电 %.1f kWh，总成本 %.2f 元，最终SOC: %.0f%%。", 
                energy, cost, finalSOC));
        
        if (costScore > 80) {
            sb.append("成本优化效果优秀。");
        }
        
        return sb.toString();
    }
}
