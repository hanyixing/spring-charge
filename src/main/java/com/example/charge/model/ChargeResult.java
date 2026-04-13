package com.example.charge.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ChargeResult {
    private boolean success;
    private String message;
    private BigDecimal totalCost;
    private BigDecimal totalEnergy;
    private Map<Integer, BigDecimal> powerPerPoint;
    private List<String> chargeSchedule;

    public ChargeResult() {
    }

    public ChargeResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
