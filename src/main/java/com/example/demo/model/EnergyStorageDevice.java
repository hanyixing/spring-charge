package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnergyStorageDevice {
    
    private Long id;
    private String name;
    private double maxPower;
    private double minPower;
    private double currentSoc;
    private double maxCapacity;
    private double efficiency;
    
    public double getAvailableChargePower() {
        return Math.min(maxPower, (maxCapacity - currentSoc) / 0.25);
    }
    
    public double getAvailableDischargePower() {
        return Math.min(maxPower, currentSoc / 0.25);
    }
}
