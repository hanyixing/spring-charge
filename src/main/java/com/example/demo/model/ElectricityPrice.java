package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 电价模型
 * 支持分时电价策略
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectricityPrice {
    
    /** 日期 */
    private LocalDate date;
    
    /** 96点电价（每15分钟一个点，单位：元/kWh） */
    private double[] priceData;
    
    /** 峰时段电价 */
    private double peakPrice;
    
    /** 平时段电价 */
    private double flatPrice;
    
    /** 谷时段电价 */
    private double valleyPrice;
    
    /** 峰时段索引列表 */
    private int[] peakTimeSlots;
    
    /** 平时段索引列表 */
    private int[] flatTimeSlots;
    
    /** 谷时段索引列表 */
    private int[] valleyTimeSlots;
    
    /**
     * 获取指定时间点的电价类型
     * @param timeSlot 时间点（0-95）
     * @return 电价类型：PEAK-峰，FLAT-平，VALLEY-谷
     */
    public String getPriceType(int timeSlot) {
        for (int slot : peakTimeSlots) {
            if (slot == timeSlot) return "PEAK";
        }
        for (int slot : valleyTimeSlots) {
            if (slot == timeSlot) return "VALLEY";
        }
        return "FLAT";
    }
    
    /**
     * 创建默认的分时电价（典型工商业电价）
     */
    public static ElectricityPrice createDefaultPrice(LocalDate date) {
        double[] prices = new double[96];
        int[] peakSlots = new int[32];   // 10:00-18:00 (40-72) = 32 slots
        int[] flatSlots = new int[40];   // 其他时段
        int[] valleySlots = new int[32]; // 23:00-07:00 (92-95, 0-27) = 32 slots
        
        double peak = 1.2;   // 峰时电价 1.2元/kWh
        double flat = 0.8;   // 平时电价 0.8元/kWh
        double valley = 0.4; // 谷时电价 0.4元/kWh
        
        int peakIdx = 0, flatIdx = 0, valleyIdx = 0;
        
        for (int i = 0; i < 96; i++) {
            // 谷时段: 23:00-07:00 (slot 92-95, 0-27)
            if (i >= 92 || i < 28) {
                prices[i] = valley;
                valleySlots[valleyIdx++] = i;
            }
            // 峰时段: 10:00-18:00 (slot 40-71)
            else if (i >= 40 && i < 72) {
                prices[i] = peak;
                peakSlots[peakIdx++] = i;
            }
            // 平时段: 其他时间
            else {
                prices[i] = flat;
                flatSlots[flatIdx++] = i;
            }
        }
        
        return ElectricityPrice.builder()
                .date(date)
                .priceData(prices)
                .peakPrice(peak)
                .flatPrice(flat)
                .valleyPrice(valley)
                .peakTimeSlots(java.util.Arrays.copyOf(peakSlots, peakIdx))
                .flatTimeSlots(java.util.Arrays.copyOf(flatSlots, flatIdx))
                .valleyTimeSlots(java.util.Arrays.copyOf(valleySlots, valleyIdx))
                .build();
    }
}
