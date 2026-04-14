package com.example.charge.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class UserChargeProfile {
    private String userId;
    private List<Map<Integer, BigDecimal>> historicalChargeCurves;
    private BigDecimal targetEnergy;
    private int preferredStartTime;
    private int preferredEndTime;
    private BigDecimal maxPower;
}
