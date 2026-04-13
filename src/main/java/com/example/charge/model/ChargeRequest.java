package com.example.charge.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ChargeRequest {
    private int currentBattery;
    private int targetBattery;
    private int startTimePoint;
    private int endTimePoint;
    private BigDecimal batteryCapacity;
    private BigDecimal maxChargePower;
    private List<ElectricityPrice> priceList;

    public ChargeRequest() {
        this.batteryCapacity = new BigDecimal("70");
        this.maxChargePower = new BigDecimal("7");
    }
}
