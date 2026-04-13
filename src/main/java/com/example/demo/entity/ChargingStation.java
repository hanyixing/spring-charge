package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargingStation {

    public static final int TOTAL_SLOTS = 96;
    public static final double SLOT_DURATION_HOURS = 0.25;

    private String stationId;

    private String stationName;

    private double totalPower;

    private List<ChargingUser> users;

    private double[] powerUsage;

    public ChargingStation(String stationId, String stationName, double totalPower) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.totalPower = totalPower;
        this.users = new ArrayList<>();
        this.powerUsage = new double[TOTAL_SLOTS];
    }

    public void addUser(ChargingUser user) {
        user.setActive(true);
        user.setAllocatedPower(new double[TOTAL_SLOTS]);
        users.add(user);
    }

    public void removeUser(String userId) {
        users.removeIf(user -> user.getUserId().equals(userId));
    }

    public void cancelCharging(String userId) {
        users.stream()
                .filter(user -> user.getUserId().equals(userId))
                .findFirst()
                .ifPresent(user -> user.setActive(false));
    }

    public void resetPowerUsage() {
        powerUsage = new double[TOTAL_SLOTS];
    }

    public double getRemainingPower(int timeSlot) {
        return totalPower - powerUsage[timeSlot];
    }
}
