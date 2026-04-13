package com.example.charge.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ElectricityPrice {
    private int timePoint;
    private BigDecimal price;
    private String periodType;

    public ElectricityPrice(int timePoint, BigDecimal price, String periodType) {
        this.timePoint = timePoint;
        this.price = price;
        this.periodType = periodType;
    }
}
