package com.example.demo.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ElectricityPriceConfig {
    
    private BigDecimal sharpPeakPrice;
    
    private BigDecimal peakPrice;
    
    private BigDecimal flatPrice;
    
    private BigDecimal valleyPrice;

    public ElectricityPriceConfig() {
        this.sharpPeakPrice = new BigDecimal("1.2");
        this.peakPrice = new BigDecimal("0.9");
        this.flatPrice = new BigDecimal("0.6");
        this.valleyPrice = new BigDecimal("0.3");
    }

    public ElectricityPriceConfig(BigDecimal sharpPeakPrice, BigDecimal peakPrice, 
                                   BigDecimal flatPrice, BigDecimal valleyPrice) {
        this.sharpPeakPrice = sharpPeakPrice;
        this.peakPrice = peakPrice;
        this.flatPrice = flatPrice;
        this.valleyPrice = valleyPrice;
    }
}
