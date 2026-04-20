package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargingUser {

    private String userId;

    private String userName;

    private int startTimeSlot;

    private int endTimeSlot;

    private double targetEnergy;

    private double currentEnergy;

    private double maxPower;

    private double[] allocatedPower;

    private boolean active;

    public double getRemainingEnergy() {
        return targetEnergy - currentEnergy;
    }

    public int getAvailableSlots() {
        return endTimeSlot - startTimeSlot + 1;
    }

    public double getMinRequiredPower() {
        int slots = getAvailableSlots();
        if (slots <= 0) return 0;
        return getRemainingEnergy() / (slots * 0.25);
    }
}
