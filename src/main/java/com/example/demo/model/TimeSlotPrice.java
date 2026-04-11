package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 时间点电价信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotPrice implements Comparable<TimeSlotPrice> {
    
    /** 时间点 (0-95) */
    private Integer timeSlot;
    
    /** 电价 */
    private BigDecimal price;
    
    /** 电价类型 */
    private PriceType priceType;
    
    @Override
    public int compareTo(TimeSlotPrice other) {
        return this.price.compareTo(other.price);
    }
    
    public enum PriceType {
        SHARP,  // 尖
        PEAK,   // 峰
        FLAT,   // 平
        VALLEY  // 谷
    }
}
