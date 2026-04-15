package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 充电推荐结果模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargingRecommendation {
    
    /** 推荐ID */
    private String recommendationId;
    
    /** 用户ID */
    private String userId;
    
    /** 推荐生成时间 */
    private LocalDateTime generateTime;
    
    /** 推荐充电日期 */
    private String targetDate;
    
    /** 96点推荐充电功率（每15分钟一个点，单位：kW） */
    private double[] recommendedPower;
    
    /** 96点电价（每15分钟一个点，单位：元/kWh） */
    private double[] electricityPrice;
    
    /** 预计总充电量（kWh） */
    private double estimatedTotalEnergy;
    
    /** 预计总成本（元） */
    private double estimatedTotalCost;
    
    /** 建议开始充电时间点（0-95） */
    private int recommendedStartSlot;
    
    /** 建议结束充电时间点（0-95） */
    private int recommendedEndSlot;
    
    /** 削峰平谷评分（0-100） */
    private double peakShavingScore;
    
    /** 成本优化评分（0-100） */
    private double costOptimizationScore;
    
    /** 用户习惯匹配度评分（0-100） */
    private double habitMatchScore;
    
    /** 综合评分（0-100） */
    private double overallScore;
    
    /** 推荐说明 */
    private String recommendationDescription;
    
    // ========== SOC相关字段 ==========
    
    /** 电池当前SOC（%） */
    private double initialSOC;
    
    /** 电池目标SOC（%） */
    private double targetSOC;
    
    /** 96点SOC变化曲线（每15分钟一个点，%） */
    private double[] socCurve;
    
    /** 电池总容量（kWh） */
    private double batteryCapacity;
    
    /** 预计离开时间（第几个15分钟点） */
    private int expectedDepartureSlot;
    
    /** SOC达成率（0-100%） */
    private double socAchievementRate;
    
    /** 是否能在离开前完成充电 */
    private boolean canCompleteBeforeDeparture;
    
    /** 充电完成时间（第几个15分钟点） */
    private int completionSlot;
}
