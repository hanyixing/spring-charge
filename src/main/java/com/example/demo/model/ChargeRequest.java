package com.example.demo.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 充电请求参数
 */
@Data
public class ChargeRequest {
    
    /** 当前电量百分比 (0-100) */
    private BigDecimal currentSoc;
    
    /** 目标电量百分比 (0-100) */
    private BigDecimal targetSoc;
    
    /** 电池总容量 (kWh) */
    private BigDecimal batteryCapacity;
    
    /** 最大充电功率 (kW) */
    private BigDecimal maxChargePower;
    
    /** 允许充电开始时间 (0-95, 每15分钟一个点) */
    private Integer startTimeSlot;
    
    /** 允许充电结束时间 (0-95, 每15分钟一个点) */
    private Integer endTimeSlot;
    
    /** 电价配置 */
    private ElectricityPrice electricityPrice;
    
    public ChargeRequest() {
        // 默认值
        this.currentSoc = new BigDecimal("20");
        this.targetSoc = new BigDecimal("80");
        this.batteryCapacity = new BigDecimal("60"); // 60度电
        this.maxChargePower = new BigDecimal("7"); // 7kW
        this.startTimeSlot = 0;
        this.endTimeSlot = 95;
        this.electricityPrice = new ElectricityPrice();
    }
}
