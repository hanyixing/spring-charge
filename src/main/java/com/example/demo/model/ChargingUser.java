package com.example.demo.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChargingUser {
    private String userId;
    private double targetEnergy;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double chargedEnergy;
    private boolean isActive;
    private int startSlot;
    private int endSlot;

    public ChargingUser(String userId, double targetEnergy, LocalDateTime startTime, LocalDateTime endTime) {
        this.userId = userId;
        this.targetEnergy = targetEnergy;
        this.startTime = startTime;
        this.endTime = endTime;
        this.chargedEnergy = 0.0;
        this.isActive = true;
        calculateTimeSlots();
    }

    private void calculateTimeSlots() {
        LocalDateTime dayStart = startTime.toLocalDate().atStartOfDay();
        long startMinutes = java.time.Duration.between(dayStart, startTime).toMinutes();
        long endMinutes = java.time.Duration.between(dayStart, endTime).toMinutes();
        this.startSlot = (int) (startMinutes / 15);
        this.endSlot = (int) (endMinutes / 15);
    }

    public int getTotalSlots() {
        return endSlot - startSlot;
    }

    public double getRequiredAveragePower() {
        return (targetEnergy - chargedEnergy) * 4;
    }
}
