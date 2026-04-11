package com.example.demo.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ChargingRequest {
    
    private Integer startPoint;
    
    private Integer endPoint;
    
    private BigDecimal currentSoc;
    
    private BigDecimal targetSoc;
    
    private BigDecimal batteryCapacity;
    
    private BigDecimal maxChargingPower;

    public ChargingRequest() {
        this.batteryCapacity = new BigDecimal("60");
        this.maxChargingPower = new BigDecimal("7");
    }
}
