package com.example.demo.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 功率调度计划
 */
@Data
public class PowerSchedule {
    
    /** 时间点索引 (0-95, 每15分钟一个点) */
    private int timeIndex;
    
    /** 时间点 */
    private LocalDateTime timePoint;
    
    /** 充电功率 (kW, 正值表示充电) */
    private double chargePower;
    
    /** 放电功率 (kW, 正值表示放电) */
    private double dischargePower;
    
    /** 净功率 (正值充电, 负值放电) */
    private double netPower;
    
    /** 电力价格 (元/kWh) */
    private double electricityPrice;
    
    /** 预计成本 */
    private double estimatedCost;
    
    /** SOC变化 */
    private double socChange;
    
    /** 累计SOC */
    private double cumulativeSoc;
    
    public PowerSchedule(int timeIndex, LocalDateTime timePoint) {
        this.timeIndex = timeIndex;
        this.timePoint = timePoint;
    }
    
    /**
     * 计算净功率
     */
    public void calculateNetPower() {
        this.netPower = chargePower - dischargePower;
    }
    
    /**
     * 计算该时段成本 (15分钟 = 0.25小时)
     */
    public void calculateCost() {
        double timeInterval = 0.25; // 15分钟 = 0.25小时
        if (netPower > 0) {
            // 充电成本
            this.estimatedCost = netPower * timeInterval * electricityPrice;
        } else {
            // 放电收益 (负成本)
            this.estimatedCost = netPower * timeInterval * electricityPrice;
        }
    }
}
