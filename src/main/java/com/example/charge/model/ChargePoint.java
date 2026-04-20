package com.example.charge.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChargePoint {
    private int timeSlot;
    private BigDecimal power;
    private BigDecimal price;
    private BigDecimal cost;
    private BigDecimal socAfterCharge;
}
