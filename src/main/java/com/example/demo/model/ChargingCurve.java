package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 充电曲线数据模型
 * 记录用户某次充电的完整曲线
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargingCurve {
    
    /** 充电记录ID */
    private String chargingId;
    
    /** 用户ID */
    private String userId;
    
    /** 充电日期 */
    private LocalDate chargingDate;
    
    /** 96点充电功率数据（每15分钟一个点，单位：kW） */
    private double[] powerData;
    
    /** 总充电量（kWh） */
    private double totalEnergy;
    
    /** 充电开始时间（第几个15分钟点，0-95） */
    private int startTimeSlot;
    
    /** 充电结束时间（第几个15分钟点，0-95） */
    private int endTimeSlot;
    
    /** 总充电时长（小时） */
    private double chargingDuration;
    
    /** 平均充电功率（kW） */
    private double avgPower;
    
    /** 最大充电功率（kW） */
    private double maxPower;
}
