package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 用户充电习惯模型
 * 基于历史充电数据分析得到的用户习惯特征
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserChargingHabit {
    
    /** 用户ID */
    private String userId;
    
    /** 历史充电次数 */
    private int totalChargingCount;
    
    /** 平均每次充电量（kWh） */
    private double avgChargingEnergy;
    
    /** 平均充电时长（小时） */
    private double avgChargingDuration;
    
    /** 平均充电功率（kW） */
    private double avgChargingPower;
    
    /** 最大充电功率（kW） */
    private double maxChargingPower;
    
    /** 96点充电概率分布（每个时间点充电的概率，0-1） */
    private double[] timeSlotProbability;
    
    /** 96点平均充电功率模板（归一化后） */
    private double[] avgPowerTemplate;
    
    /** 首选充电开始时间段（如：晚上22:00-24:00） */
    private List<TimeRange> preferredStartTimeRanges;
    
    /** 充电频率（次/周） */
    private double chargingFrequency;
    
    /** 工作日/周末充电偏好 */
    private Map<String, Double> dayTypePreference;
    
    /** 习惯稳定性评分（0-100，越高表示习惯越稳定） */
    private double habitStabilityScore;
    
    /**
     * 时间段范围
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeRange {
        private int startSlot;
        private int endSlot;
        private double probability;
        private String description;
    }
}
