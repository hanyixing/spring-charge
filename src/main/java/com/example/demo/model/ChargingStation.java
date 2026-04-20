package com.example.demo.model;

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
    
    private String stationId;
    private String stationName;
    private double totalPower;
    @Builder.Default
    private List<ChargingUser> users = new ArrayList<>();
    @Builder.Default
    private TimeSlotPower[] timeSlotPowers = new TimeSlotPower[96];
    private int currentTimeSlot;

    public static ChargingStation create(String stationName, double totalPower) {
        ChargingStation station = ChargingStation.builder()
                .stationId(java.util.UUID.randomUUID().toString())
                .stationName(stationName)
                .totalPower(totalPower)
                .currentTimeSlot(0)
                .build();
        
        for (int i = 0; i < 96; i++) {
            station.timeSlotPowers[i] = TimeSlotPower.builder()
                    .slotIndex(i)
                    .build();
        }
        return station;
    }

    public double getCurrentTotalPower() {
        if (currentTimeSlot >= 0 && currentTimeSlot < 96) {
            return timeSlotPowers[currentTimeSlot].getTotalPower();
        }
        return 0;
    }

    public double getAvailablePower() {
        return totalPower - getCurrentTotalPower();
    }

    public List<ChargingUser> getActiveUsers() {
        List<ChargingUser> activeUsers = new ArrayList<>();
        for (ChargingUser user : users) {
            if (user.isActive() && !user.isCompleted()) {
                activeUsers.add(user);
            }
        }
        return activeUsers;
    }

    public List<ChargingUser> getUsersInTimeRange(int timeSlot) {
        List<ChargingUser> result = new ArrayList<>();
        for (ChargingUser user : users) {
            if (user.isActive() && !user.isCompleted() && user.isInTimeRange(timeSlot)) {
                result.add(user);
            }
        }
        return result;
    }
}
