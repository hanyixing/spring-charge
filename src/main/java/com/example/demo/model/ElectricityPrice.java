package com.example.demo.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 电价配置类
 * 尖峰平谷四种电价
 */
@Data
public class ElectricityPrice {
    
    /** 尖时电价 (最高) */
    private BigDecimal sharpPrice;
    
    /** 峰时电价 */
    private BigDecimal peakPrice;
    
    /** 平时电价 */
    private BigDecimal flatPrice;
    
    /** 谷时电价 (最低) */
    private BigDecimal valleyPrice;
    
    public ElectricityPrice() {
        // 默认电价配置
        this.sharpPrice = new BigDecimal("1.2");
        this.peakPrice = new BigDecimal("0.9");
        this.flatPrice = new BigDecimal("0.6");
        this.valleyPrice = new BigDecimal("0.3");
    }
    
    public ElectricityPrice(BigDecimal sharpPrice, BigDecimal peakPrice, 
                           BigDecimal flatPrice, BigDecimal valleyPrice) {
        this.sharpPrice = sharpPrice;
        this.peakPrice = peakPrice;
        this.flatPrice = flatPrice;
        this.valleyPrice = valleyPrice;
    }
}
