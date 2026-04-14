package com.example.demo.entity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "reward")
public class RewardConfig {
    private double rewardRate = 0.5;
    private List<String> valleyPeriods = Arrays.asList("23:00-07:00", "12:00-14:00");
    private List<String> peakPeriods = Arrays.asList("08:00-11:00", "18:00-22:00");
}
