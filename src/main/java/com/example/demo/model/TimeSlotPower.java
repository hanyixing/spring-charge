package com.example.demo.model;

import lombok.Data;

@Data
public class TimeSlotPower {
    private int timeSlot;
    private double chargePower;
    private double dischargePower;
    private double electricityPrice;
    private double demand;

    public TimeSlotPower() {
    }

    public TimeSlotPower(int timeSlot, double electricityPrice) {
        this.timeSlot = timeSlot;
        this.electricityPrice = electricityPrice;
        this.chargePower = 0;
        this.dischargePower = 0;
        this.demand = 0;
    }
}
