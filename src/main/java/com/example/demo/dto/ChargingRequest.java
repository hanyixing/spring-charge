package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargingRequest {

    @NotNull(message = "当前电量不能为空")
    @Min(value = 0, message = "当前电量不能小于0")
    @Max(value = 100, message = "当前电量不能大于100")
    private Double currentSoc;

    @NotNull(message = "目标电量不能为空")
    @Min(value = 0, message = "目标电量不能小于0")
    @Max(value = 100, message = "目标电量不能大于100")
    private Double targetSoc;

    @NotNull(message = "开始时间点不能为空")
    @Min(value = 0, message = "开始时间点不能小于0")
    @Max(value = 95, message = "开始时间点不能大于95")
    private Integer startPoint;

    @NotNull(message = "结束时间点不能为空")
    @Min(value = 0, message = "结束时间点不能小于0")
    @Max(value = 95, message = "结束时间点不能大于95")
    private Integer endPoint;

    @NotNull(message = "电池容量不能为空")
    @Min(value = 1, message = "电池容量必须大于0")
    private Double batteryCapacity;

    @NotNull(message = "最大充电功率不能为空")
    @Min(value = 1, message = "最大充电功率必须大于0")
    private Double maxChargingPower;
}
