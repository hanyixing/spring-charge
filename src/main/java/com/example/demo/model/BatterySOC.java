package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 电池SOC（State of Charge）模型
 * 管理电池充电状态相关参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatterySOC {
    
    /** 电池总容量（kWh） */
    private double totalCapacity;
    
    /** 当前SOC（0-100%） */
    private double currentSOC;
    
    /** 目标SOC（0-100%） */
    private double targetSOC;
    
    /** 最小允许SOC（0-100%，保护电池） */
    private double minAllowedSOC;
    
    /** 最大允许SOC（0-100%，保护电池） */
    private double maxAllowedSOC;
    
    /** 充电效率（0-1，考虑充电损耗） */
    private double chargingEfficiency;
    
    /** 预计离开时间（第几个15分钟点，0-95） */
    private int expectedDepartureSlot;
    
    /**
     * 计算需要充电的能量（kWh）
     * 考虑充电效率
     */
    public double calculateRequiredEnergy() {
        double socDiff = targetSOC - currentSOC;
        double energyNeeded = (socDiff / 100.0) * totalCapacity;
        // 考虑充电效率
        return energyNeeded / chargingEfficiency;
    }
    
    /**
     * 计算当前剩余电量（kWh）
     */
    public double getCurrentEnergy() {
        return (currentSOC / 100.0) * totalCapacity;
    }
    
    /**
     * 计算目标电量（kWh）
     */
    public double getTargetEnergy() {
        return (targetSOC / 100.0) * totalCapacity;
    }
    
    /**
     * 根据充电能量计算SOC变化
     * @param chargedEnergy 已充电能量（kWh）
     * @return 新的SOC值
     */
    public double calculateNewSOC(double chargedEnergy) {
        double actualEnergy = chargedEnergy * chargingEfficiency;
        double newEnergy = getCurrentEnergy() + actualEnergy;
        double newSOC = (newEnergy / totalCapacity) * 100.0;
        return Math.min(newSOC, maxAllowedSOC);
    }
    
    /**
     * 检查SOC是否在允许范围内
     */
    public boolean isSOCValid(double soc) {
        return soc >= minAllowedSOC && soc <= maxAllowedSOC;
    }
    
    /**
     * 获取SOC差值百分比
     */
    public double getSOCDifference() {
        return targetSOC - currentSOC;
    }
    
    /**
     * 判断是否需要在指定时间内完成充电
     */
    public boolean hasTimeConstraint() {
        return expectedDepartureSlot >= 0;
    }
    
    /**
     * 计算最晚开始充电时间
     * @param chargingPower 充电功率（kW）
     * @return 最晚开始充电的时间点（0-95）
     */
    public int calculateLatestStartSlot(double chargingPower) {
        if (!hasTimeConstraint() || chargingPower <= 0) {
            return 0;
        }
        
        double requiredEnergy = calculateRequiredEnergy();
        double chargingHours = requiredEnergy / chargingPower;
        int requiredSlots = (int) Math.ceil(chargingHours * 4); // 转换为15分钟点数
        
        int latestStart = expectedDepartureSlot - requiredSlots;
        return Math.max(0, latestStart);
    }
    
    /**
     * 创建默认电池配置（典型电动车）
     */
    public static BatterySOC createDefault() {
        return BatterySOC.builder()
                .totalCapacity(60.0)      // 60kWh电池
                .currentSOC(20.0)         // 当前20%
                .targetSOC(80.0)          // 目标80%
                .minAllowedSOC(10.0)      // 最低10%
                .maxAllowedSOC(90.0)      // 最高90%
                .chargingEfficiency(0.92) // 92%充电效率
                .expectedDepartureSlot(88) // 预计22:00离开
                .build();
    }
    
    /**
     * 创建电池配置（带参数）
     */
    public static BatterySOC create(double capacity, double currentSOC, 
            double targetSOC, int departureSlot) {
        return BatterySOC.builder()
                .totalCapacity(capacity)
                .currentSOC(currentSOC)
                .targetSOC(targetSOC)
                .minAllowedSOC(10.0)
                .maxAllowedSOC(90.0)
                .chargingEfficiency(0.92)
                .expectedDepartureSlot(departureSlot)
                .build();
    }
}
