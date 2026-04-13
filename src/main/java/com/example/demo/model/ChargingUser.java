package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChargingUser {
    private String userId;
    private double currentEnergy;
    private double targetEnergy;
    private int startTimeSlot;
    private int endTimeSlot;
    private double maxChargingPower;
    private boolean active;
    private LocalDateTime createTime;
    
    public static ChargingUser create(double currentEnergy, double targetEnergy, 
                                       int startTimeSlot, int endTimeSlot, 
                                       double maxChargingPower) {
        ChargingUser user = new ChargingUser();
        user.setUserId(UUID.randomUUID().toString().substring(0, 8));
        user.setCurrentEnergy(currentEnergy);
        user.setTargetEnergy(targetEnergy);
        user.setStartTimeSlot(startTimeSlot);
        user.setEndTimeSlot(endTimeSlot);
        user.setMaxChargingPower(maxChargingPower);
        user.setActive(true);
        user.setCreateTime(LocalDateTime.now());
        return user;
    }
    
    public double getRequiredEnergy() {
        return Math.max(0, targetEnergy - currentEnergy);
    }
    
    public int getAvailableTimeSlots() {
        return endTimeSlot - startTimeSlot + 1;
    }
    
    public double getMinRequiredPower() {
        int slots = getAvailableTimeSlots();
        if (slots <= 0) return 0;
        double slotEnergy = 0.25;
        return getRequiredEnergy() / (slots * slotEnergy);
    }
}
