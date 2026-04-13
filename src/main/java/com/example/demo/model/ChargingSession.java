package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargingSession {
    
    private String sessionId;
    private String userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double chargingPowerKw;
    private double totalChargingKwh;
    private double totalRewardAmount;
    private int totalSlots;
    private int valleySlots;
    private int peakSlots;
    private int normalSlots;
    private double valleyChargingKwh;
    private double peakChargingKwh;
    private double normalChargingKwh;
}
