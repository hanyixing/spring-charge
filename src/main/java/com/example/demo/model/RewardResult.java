package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardResult {
    
    private String userId;
    private double totalChargingKwh;
    private double totalRewardAmount;
    private double valleyChargingKwh;
    private double peakChargingKwh;
    private double normalChargingKwh;
    private List<ChargingRecord> chargingRecords;
}
