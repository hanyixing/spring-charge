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
public class ChargingRecord {
    
    private String userId;
    private double chargingKwh;
    private LocalDateTime chargingStartTime;
    private LocalDateTime chargingEndTime;
    private int timeSlotIndex;
    private double rewardAmount;
    private String timeSlotType;
}
