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
public class SlotChargingDetail {
    
    private int slotIndex;
    private LocalDateTime slotStartTime;
    private LocalDateTime slotEndTime;
    private String slotType;
    private double rewardRate;
    private double chargingKwh;
    private double rewardAmount;
    private double chargingMinutes;
}
