package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotConfig {
    
    private int slotIndex;
    private String startTime;
    private String endTime;
    private String slotType;
    private double rewardRate;
    
    public static final int TOTAL_SLOTS = 96;
    public static final int SLOT_DURATION_MINUTES = 15;
    
    public static final String SLOT_TYPE_VALLEY = "VALLEY";
    public static final String SLOT_TYPE_PEAK = "PEAK";
    public static final String SLOT_TYPE_NORMAL = "NORMAL";
}
