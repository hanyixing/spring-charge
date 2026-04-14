package com.example.demo.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 优化结果
 */
@Data
public class OptimizationResult {
    
    /** 总成本 */
    private double totalCost;
    
    /** 总收益 */
    private double totalRevenue;
    
    /** 净利润 */
    private double netProfit;
    
    /** 总充电量 (kWh) */
    private double totalChargeEnergy;
    
    /** 总放电量 (kWh) */
    private double totalDischargeEnergy;
    
    /** 各设备调度计划 Map<deviceId, List<PowerSchedule>> */
    private Map<String, List<PowerSchedule>> deviceSchedules;
    
    /** 总调度计划 */
    private List<PowerSchedule> totalSchedule;
    
    /** 初始总SOC */
    private double initialTotalSoc;
    
    /** 最终总SOC */
    private double finalTotalSoc;
    
    /** SOC变化 */
    private double socChange;
    
    /** 是否满足所有约束 */
    private boolean constraintsSatisfied;
    
    /** 约束违反信息 */
    private List<String> constraintViolations;
    
    /** 优化耗时 (ms) */
    private long optimizationTimeMs;
    
    /**
     * 计算汇总信息
     */
    public void calculateSummary() {
        this.netProfit = totalRevenue - totalCost;
        
        if (totalSchedule != null && !totalSchedule.isEmpty()) {
            PowerSchedule first = totalSchedule.get(0);
            PowerSchedule last = totalSchedule.get(totalSchedule.size() - 1);
            this.initialTotalSoc = first.getCumulativeSoc();
            this.finalTotalSoc = last.getCumulativeSoc();
            this.socChange = finalTotalSoc - initialTotalSoc;
        }
    }
}
