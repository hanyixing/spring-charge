package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {
    
    private int slotIndex;
    private String startTime;
    private String endTime;
    private double electricityPrice;
    private double predictedDemand;
    
    public static TimeSlot create(int index) {
        int hour = index / 4;
        int minute = (index % 4) * 15;
        String startTime = String.format("%02d:%02d", hour, minute);
        int endMinute = minute + 15;
        int endHour = hour;
        if (endMinute >= 60) {
            endMinute = 0;
            endHour = (endHour + 1) % 24;
        }
        String endTime = String.format("%02d:%02d", endHour, endMinute);
        return TimeSlot.builder()
                .slotIndex(index)
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }
}
