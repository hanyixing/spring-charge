package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 电价配置实体类
 * 支持尖峰平谷四种电价类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectricityPrice {

    /**
     * 电价类型：尖峰(JIAN)、峰(FENG)、平(PING)、谷(GU)
     */
    private PriceType type;

    /**
     * 电价（元/kWh）
     */
    private double price;

    /**
     * 开始时间（点索引，0-95）
     */
    private int startPoint;

    /**
     * 结束时间（点索引，0-95，不包含）
     */
    private int endPoint;

    /**
     * 电价类型枚举
     */
    public enum PriceType {
        JIAN("尖", "尖峰电价", 4),
        FENG("峰", "峰时电价", 3),
        PING("平", "平时电价", 2),
        GU("谷", "谷时电价", 1);

        private final String code;
        private final String desc;
        private final int priority;

        PriceType(String code, String desc, int priority) {
            this.code = code;
            this.desc = desc;
            this.priority = priority;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }

        public int getPriority() {
            return priority;
        }
    }
}
