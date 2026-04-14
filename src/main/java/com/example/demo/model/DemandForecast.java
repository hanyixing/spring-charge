package com.example.demo.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 需求预测
 */
@Data
public class DemandForecast {
    
    /** 时间点索引 */
    private int timeIndex;
    
    /** 时间点 */
    private LocalDateTime timePoint;
    
    /** 预测充电需求 (kW) */
    private double predictedChargeDemand;
    
    /** 预测放电需求 (kW) */
    private double predictedDischargeDemand;
    
    /** 预测净需求 (正值充电, 负值放电) */
    private double predictedNetDemand;
    
    /** 预测置信度 0-1 */
    private double confidence;
    
    /** 电力价格预测 */
    private double predictedPrice;
    
    public DemandForecast(int timeIndex, LocalDateTime timePoint) {
        this.timeIndex = timeIndex;
        this.timePoint = timePoint;
    }
    
    /**
     * 计算净需求
     */
    public void calculateNetDemand() {
        this.predictedNetDemand = predictedChargeDemand - predictedDischargeDemand;
    }
}
