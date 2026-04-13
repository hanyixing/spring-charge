package com.example.demo.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ChargeSession {
    private String userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal energyKwh;
}
