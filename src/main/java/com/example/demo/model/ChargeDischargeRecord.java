package com.example.demo.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChargeDischargeRecord {
    private LocalDateTime timestamp;
    private String deviceId;
    private double power;
    private double soc;
    private double electricityPrice;

    public ChargeDischargeRecord() {
    }

    public ChargeDischargeRecord(LocalDateTime timestamp, String deviceId, double power, double soc, double electricityPrice) {
        this.timestamp = timestamp;
        this.deviceId = deviceId;
        this.power = power;
        this.soc = soc;
        this.electricityPrice = electricityPrice;
    }
}
