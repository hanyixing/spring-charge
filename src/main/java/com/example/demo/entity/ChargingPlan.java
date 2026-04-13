package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 充电计划结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargingPlan {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 消息
     */
    private String message;

    /**
     * 需要充电的电量（kWh）
     */
    private double requiredEnergy;

    /**
     * 预计总成本（元）
     */
    private double totalCost;

    /**
     * 充电时长（分钟）
     */
    private int chargingDuration;

    /**
     * 每个时间点的充电功率（kW）
     * key: 点索引(0-95), value: 功率(kW)
     */
    private Map<Integer, Double> powerDistribution;

    /**
     * 每个时间点的充电成本（元）
     * key: 点索引(0-95), value: 成本(元)
     */
    private Map<Integer, Double> costDistribution;

    /**
     * 每个时间点的电价信息
     * key: 点索引(0-95), value: 电价类型
     */
    private Map<Integer, ElectricityPrice.PriceType> priceTypeDistribution;

    /**
     * 充电时段详情列表
     */
    private List<ChargingSegment> segments;

    /**
     * 充电时段详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChargingSegment {
        /**
         * 开始时间点（0-95）
         */
        private int startPoint;

        /**
         * 结束时间点（0-95，不包含）
         */
        private int endPoint;

        /**
         * 开始时间字符串（HH:mm）
         */
        private String startTime;

        /**
         * 结束时间字符串（HH:mm）
         */
        private String endTime;

        /**
         * 充电功率（kW）
         */
        private double power;

        /**
         * 该时段充电电量（kWh）
         */
        private double energy;

        /**
         * 该时段成本（元）
         */
        private double cost;

        /**
         * 电价类型
         */
        private ElectricityPrice.PriceType priceType;

        /**
         * 电价（元/kWh）
         */
        private double price;
    }
}
