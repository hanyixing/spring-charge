package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargingUser {
    
    private String userId;
    private String userName;
    private double targetEnergy;
    private double currentEnergy;
    private int startTimeSlot;
    private int endTimeSlot;
    private double minPower;
    private double maxPower;
    private double currentPower;
    private boolean isActive;

    public static ChargingUser create(String userName, double targetEnergy, 
                                       int startTimeSlot, int endTimeSlot,
                                       double minPower, double maxPower) {
        return ChargingUser.builder()
                .userId(UUID.randomUUID().toString())
                .userName(userName)
                .targetEnergy(targetEnergy)
                .currentEnergy(0.0)
                .startTimeSlot(startTimeSlot)
                .endTimeSlot(endTimeSlot)
                .minPower(minPower)
                .maxPower(maxPower)
                .currentPower(0.0)
                .isActive(true)
                .build();
    }

    public double getRemainingEnergy() {
        return Math.max(0, targetEnergy - currentEnergy);
    }

    public int getRemainingTimeSlots(int currentTimeSlot) {
        return Math.max(0, endTimeSlot - currentTimeSlot + 1);
    }

    public boolean isInTimeRange(int timeSlot) {
        return timeSlot >= startTimeSlot && timeSlot <= endTimeSlot;
    }

    public boolean isCompleted() {
        return currentEnergy >= targetEnergy;
    }

    public double getUrgency(int currentTimeSlot) {
        int remainingSlots = getRemainingTimeSlots(currentTimeSlot);
        if (remainingSlots <= 0) {
            return Double.MAX_VALUE;
        }
        return getRemainingEnergy() / remainingSlots;
    }
}
