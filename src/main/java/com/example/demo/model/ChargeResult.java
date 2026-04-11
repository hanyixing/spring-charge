package com.example.demo.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ChargeResult {
    private List<Double> chargePowers;
    private BigDecimal totalCost;
    private double totalEnergy;
    private boolean success;
    private String message;

    public ChargeResult() {
        this.chargePowers = new ArrayList<>(96);
        for (int i = 0; i < 96; i++) {
            this.chargePowers.add(0.0);
        }
        this.totalCost = BigDecimal.ZERO;
        this.totalEnergy = 0.0;
        this.success = false;
    }

    public void setChargePower(int index, double power) {
        if (index >= 0 && index < 96) {
            this.chargePowers.set(index, power);
        }
    }

    public double getChargePower(int index) {
        if (index >= 0 && index < 96) {
            return this.chargePowers.get(index);
        }
        return 0.0;
    }
}
