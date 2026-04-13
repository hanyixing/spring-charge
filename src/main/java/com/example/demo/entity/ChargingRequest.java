package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 充电请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargingRequest {

    /**
     * 当前电量百分比（0-100）
     */
    @NotNull(message = "当前电量不能为空")
    @Min(value = 0, message = "当前电量不能小于0")
    @Max(value = 100, message = "当前电量不能大于100")
    private Double currentSoc;

    /**
     * 目标电量百分比（0-100）
     */
    @NotNull(message = "目标电量不能为空")
    @Min(value = 0, message = "目标电量不能小于0")
    @Max(value = 100, message = "目标电量不能大于100")
    private Double targetSoc;

    /**
     * 充电开始时间（点索引，0-95）
     */
    @NotNull(message = "充电开始时间不能为空")
    @Min(value = 0, message = "充电开始时间不能小于0")
    @Max(value = 95, message = "充电开始时间不能大于95")
    private Integer startTimePoint;

    /**
     * 充电结束时间（点索引，0-95，不包含）
     */
    @NotNull(message = "充电结束时间不能为空")
    @Min(value = 1, message = "充电结束时间不能小于1")
    @Max(value = 96, message = "充电结束时间不能大于96")
    private Integer endTimePoint;

    /**
     * 电池容量（kWh）
     */
    @NotNull(message = "电池容量不能为空")
    @Min(value = 1, message = "电池容量必须大于0")
    private Double batteryCapacity;

    /**
     * 最大充电功率（kW）
     */
    @NotNull(message = "最大充电功率不能为空")
    @Min(value = 1, message = "最大充电功率必须大于0")
    private Double maxChargingPower;

    /**
     * 验证请求参数是否合法
     */
    public boolean isValid() {
        if (currentSoc == null || targetSoc == null ||
            startTimePoint == null || endTimePoint == null ||
            batteryCapacity == null || maxChargingPower == null) {
            return false;
        }
        // 目标电量必须大于当前电量
        if (targetSoc <= currentSoc) {
            return false;
        }
        // 结束时间必须大于开始时间
        if (endTimePoint <= startTimePoint) {
            return false;
        }
        return true;
    }
}
