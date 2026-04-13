package com.example.charge.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PriceUpdateRequest {
    private Integer timePoint;
    private BigDecimal price;
    private String periodType;
    private List<ElectricityPrice> prices;
}
