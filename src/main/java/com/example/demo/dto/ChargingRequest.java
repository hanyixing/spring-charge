package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChargingRequest {
    private Integer startPoint;
    private Integer endPoint;
    private Double chargingKwh;
    private Double rewardPerKwh;
    private Integer days;
}
