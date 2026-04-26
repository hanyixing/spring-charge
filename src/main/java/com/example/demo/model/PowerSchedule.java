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
public class PowerSchedule {
    
    private Long deviceId;
    private String deviceName;
    @Builder.Default
    private List<Double> chargePower = new ArrayList<>();
    @Builder.Default
    private List<Double> dischargePower = new ArrayList<>();
    @Builder.Default
    private List<Double> socList = new ArrayList<>();
    private double totalCost;
    private double totalDemandMet;
    
    public void initialize(int slots) {
        chargePower = new ArrayList<>();
        dischargePower = new ArrayList<>();
        socList = new ArrayList<>();
        for (int i = 0; i < slots; i++) {
            chargePower.add(0.0);
            dischargePower.add(0.0);
            socList.add(0.0);
        }
    }
}
