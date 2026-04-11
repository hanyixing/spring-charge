package com.example.demo.service;

import com.example.demo.model.ElectricityPrice;
import com.example.demo.model.TimeSlotPrice;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 电价配置服务
 * 管理96个时间点的电价配置
 */
@Service
public class ElectricityPriceService {
    
    // 默认电价配置
    private ElectricityPrice defaultPrice;
    
    public ElectricityPriceService() {
        this.defaultPrice = new ElectricityPrice();
    }
    
    /**
     * 获取默认电价配置
     */
    public ElectricityPrice getDefaultPrice() {
        return defaultPrice;
    }
    
    /**
     * 设置默认电价配置
     */
    public void setDefaultPrice(ElectricityPrice price) {
        this.defaultPrice = price;
    }
    
    /**
     * 获取96个时间点的电价配置
     * 时间段定义：
     * - 谷时: 00:00-08:00 (0-31)
     * - 平时: 08:00-10:00 (32-39), 15:00-17:00 (60-67), 21:00-24:00 (84-95)
     * - 峰时: 10:00-15:00 (40-59), 19:00-21:00 (76-83)
     * - 尖时: 17:00-19:00 (68-75)
     */
    public List<TimeSlotPrice> getTimeSlotPrices(ElectricityPrice price) {
        List<TimeSlotPrice> prices = new ArrayList<>(96);
        
        for (int i = 0; i < 96; i++) {
            TimeSlotPrice slotPrice = new TimeSlotPrice();
            slotPrice.setTimeSlot(i);
            
            // 根据时间段设置电价类型和价格
            if (isValleyTime(i)) {
                slotPrice.setPrice(price.getValleyPrice());
                slotPrice.setPriceType(TimeSlotPrice.PriceType.VALLEY);
            } else if (isFlatTime(i)) {
                slotPrice.setPrice(price.getFlatPrice());
                slotPrice.setPriceType(TimeSlotPrice.PriceType.FLAT);
            } else if (isPeakTime(i)) {
                slotPrice.setPrice(price.getPeakPrice());
                slotPrice.setPriceType(TimeSlotPrice.PriceType.PEAK);
            } else if (isSharpTime(i)) {
                slotPrice.setPrice(price.getSharpPrice());
                slotPrice.setPriceType(TimeSlotPrice.PriceType.SHARP);
            }
            
            prices.add(slotPrice);
        }
        
        return prices;
    }
    
    /**
     * 判断是否为谷时 (00:00-08:00)
     */
    private boolean isValleyTime(int timeSlot) {
        return timeSlot >= 0 && timeSlot <= 31;
    }
    
    /**
     * 判断是否为平时
     * 08:00-10:00, 15:00-17:00, 21:00-24:00
     */
    private boolean isFlatTime(int timeSlot) {
        return (timeSlot >= 32 && timeSlot <= 39) ||
               (timeSlot >= 60 && timeSlot <= 67) ||
               (timeSlot >= 84 && timeSlot <= 95);
    }
    
    /**
     * 判断是否为峰时
     * 10:00-15:00, 19:00-21:00
     */
    private boolean isPeakTime(int timeSlot) {
        return (timeSlot >= 40 && timeSlot <= 59) ||
               (timeSlot >= 76 && timeSlot <= 83);
    }
    
    /**
     * 判断是否为尖时 (17:00-19:00)
     */
    private boolean isSharpTime(int timeSlot) {
        return timeSlot >= 68 && timeSlot <= 75;
    }
    
    /**
     * 将时间点转换为时间字符串 (HH:mm格式)
     */
    public String timeSlotToString(int timeSlot) {
        int totalMinutes = timeSlot * 15;
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }
}
