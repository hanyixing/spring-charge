package com.example.demo.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 充电计划结果
 */
@Data
public class ChargePlan {
    
    /** 每个时间点的充电功率 (kW) 共96个点 */
    private List<BigDecimal> powerSlots;
    
    /** 总充电量 (kWh) */
    private BigDecimal totalEnergy;
    
    /** 总充电成本 (元) */
    private BigDecimal totalCost;
    
    /** 预计充电时长 (分钟) */
    private Integer durationMinutes;
    
    /** 实际开始时间点 */
    private Integer actualStartSlot;
    
    /** 实际结束时间点 */
    private Integer actualEndSlot;
    
    /** 每个时间点的电价 */
    private List<BigDecimal> priceSlots;
    
    /** 充电完成后的SOC */
    private BigDecimal finalSoc;
}
