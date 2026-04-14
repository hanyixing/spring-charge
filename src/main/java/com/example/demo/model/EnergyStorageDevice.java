package com.example.demo.model;

import lombok.Data;

@Data
public class EnergyStorageDevice {
    private String id;
    private String name;
    private double maxChargePower;
    private double minChargePower;
    private double maxDischargePower;
    private double minDischargePower;
    private double currentSOC;
    private double maxCapacity;
    private double chargeEfficiency;
    private double dischargeEfficiency;

    public EnergyStorageDevice() {
    }

    public EnergyStorageDevice(String id, String name, double maxChargePower, double minChargePower,
                               double maxDischargePower, double minDischargePower, double maxCapacity) {
        this.id = id;
        this.name = name;
        this.maxChargePower = maxChargePower;
        this.minChargePower = minChargePower;
        this.maxDischargePower = maxDischargePower;
        this.minDischargePower = minDischargePower;
        this.maxCapacity = maxCapacity;
        this.currentSOC = 0.5;
        this.chargeEfficiency = 0.95;
        this.dischargeEfficiency = 0.95;
    }
}
