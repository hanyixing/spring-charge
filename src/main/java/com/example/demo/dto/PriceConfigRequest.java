package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceConfigRequest {

    @NotNull(message = "尖峰电价不能为空")
    @Min(value = 0, message = "电价不能为负数")
    private Double sharpPrice;

    @NotNull(message = "峰电价不能为空")
    @Min(value = 0, message = "电价不能为负数")
    private Double peakPrice;

    @NotNull(message = "平电价不能为空")
    @Min(value = 0, message = "电价不能为负数")
    private Double flatPrice;

    @NotNull(message = "谷电价不能为空")
    @Min(value = 0, message = "电价不能为负数")
    private Double valleyPrice;
}
