package com.example.demo.model;

import lombok.Data;

/**
 * 储能设备模型
 */
@Data
public class StorageDevice {
    
    /** 设备ID */
    private String deviceId;
    
    /** 设备名称 */
    private String deviceName;
    
    /** 最大充电功率 (kW) */
    private double maxChargePower;
    
    /** 最大放电功率 (kW) */
    private double maxDischargePower;
    
    /** 最小充电功率 (kW) */
    private double minChargePower;
    
    /** 最小放电功率 (kW) */
    private double minDischargePower;
    
    /** 当前SOC (State of Charge) 0-1 */
    private double currentSoc;
    
    /** 最大SOC */
    private double maxSoc;
    
    /** 最小SOC */
    private double minSoc;
    
    /** 额定容量 (kWh) */
    private double ratedCapacity;
    
    /** 充电效率 */
    private double chargeEfficiency;
    
    /** 放电效率 */
    private double dischargeEfficiency;
    
    /** 设备状态: IDLE, CHARGING, DISCHARGING */
    private DeviceStatus status;
    
    public enum DeviceStatus {
        IDLE, CHARGING, DISCHARGING
    }
    
    /**
     * 获取当前可用充电容量
     */
    public double getAvailableChargeCapacity() {
        return ratedCapacity * (maxSoc - currentSoc);
    }
    
    /**
     * 获取当前可用放电容量
     */
    public double getAvailableDischargeCapacity() {
        return ratedCapacity * (currentSoc - minSoc);
    }
    
    /**
     * 检查是否可以充电指定功率
     */
    public boolean canCharge(double power) {
        return power >= minChargePower && power <= maxChargePower;
    }
    
    /**
     * 检查是否可以放电指定功率
     */
    public boolean canDischarge(double power) {
        return power >= minDischargePower && power <= maxDischargePower;
    }
}
