package com.example.charge.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElectricityPrice {
    private int timeSlot;
    private BigDecimal price;
    private String periodType;
}
