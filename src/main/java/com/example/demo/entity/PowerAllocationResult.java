package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PowerAllocationResult {

    private boolean success;

    private String message;

    private double totalStationPower;

    private double[] totalPowerUsage;

    private List<UserAllocationDetail> userAllocations;

    private Map<Integer, Double> timeSlotPowerMap;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserAllocationDetail {
        private String userId;
        private String userName;
        private int startTimeSlot;
        private int endTimeSlot;
        private double targetEnergy;
        private double actualEnergy;
        private double[] allocatedPower;
        private double maxPower;
        private boolean satisfied;
    }
}
