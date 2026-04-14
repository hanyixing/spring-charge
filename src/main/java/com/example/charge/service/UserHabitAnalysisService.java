package com.example.charge.service;

import com.example.charge.model.ChargeRecommendationRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserHabitAnalysisService {

    public List<BigDecimal> analyzeUserHabit(ChargeRecommendationRequest request) {
        List<List<BigDecimal>> historicalData = request.getHistoricalData();
        List<BigDecimal> habitCurve = new ArrayList<>();
        
        if (historicalData == null || historicalData.isEmpty()) {
            for (int i = 0; i < 96; i++) {
                habitCurve.add(BigDecimal.ZERO);
            }
            return habitCurve;
        }
        
        for (int i = 0; i < 96; i++) {
            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;
            
            for (List<BigDecimal> dayData : historicalData) {
                if (i < dayData.size()) {
                    sum = sum.add(dayData.get(i));
                    count++;
                }
            }
            
            if (count > 0) {
                habitCurve.add(sum.divide(new BigDecimal(count), 4, BigDecimal.ROUND_HALF_UP));
            } else {
                habitCurve.add(BigDecimal.ZERO);
            }
        }
        
        return habitCurve;
    }

    public int[] getPreferredChargeWindow(List<BigDecimal> habitCurve) {
        BigDecimal threshold = new BigDecimal("0.1");
        int start = -1;
        int end = -1;
        
        for (int i = 0; i < 96; i++) {
            if (habitCurve.get(i).compareTo(threshold) > 0) {
                if (start == -1) {
                    start = i;
                }
                end = i;
            }
        }
        
        if (start == -1) {
            start = 72;
            end = 95;
        }
        
        return new int[]{start, end};
    }
}
