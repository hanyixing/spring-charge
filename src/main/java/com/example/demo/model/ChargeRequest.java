package com.example.demo.model;

import lombok.Data;

@Data
public class ChargeRequest {
    private int currentBattery;
    private int targetBattery;
    private int startTimeIndex;
    private int endTimeIndex;
    private double maxChargePower;
    private double batteryCapacity;

    public ChargeRequest() {
        this.maxChargePower = 7.0;
        this.batteryCapacity = 50.0;
    }

    public ChargeRequest(int currentBattery, int targetBattery, int startTimeIndex, int endTimeIndex) {
        this.currentBattery = currentBattery;
        this.targetBattery = targetBattery;
        this.startTimeIndex = startTimeIndex;
        this.endTimeIndex = endTimeIndex;
        this.maxChargePower = 7.0;
        this.batteryCapacity = 50.0;
    }

    public double getRequiredEnergy() {
        return batteryCapacity * (targetBattery - currentBattery) / 100.0;
    }
}
