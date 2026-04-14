package com.example.demo.entity;

import lombok.Data;

@Data
public class ChargingPoint {
    private int index;
    private int hour;
    private int minute;
    private boolean rewardPeriod;
    private double rewardRate;
    private String periodType;

    public ChargingPoint(int index, int hour, int minute) {
        this.index = index;
        this.hour = hour;
        this.minute = minute;
        this.rewardPeriod = false;
        this.rewardRate = 0.0;
        this.periodType = "normal";
    }
}
