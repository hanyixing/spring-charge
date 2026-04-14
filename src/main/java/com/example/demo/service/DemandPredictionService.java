package com.example.demo.service;

import com.example.demo.model.ChargeDischargeRecord;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DemandPredictionService {

    public List<Double> predictFutureDemand(List<ChargeDischargeRecord> historicalData, int numSlots) {
        if (historicalData == null || historicalData.isEmpty()) {
            return generateDefaultDemand(numSlots);
        }

        Map<Integer, List<Double>> slotDemandMap = groupByTimeSlot(historicalData);

        List<Double> predictedDemand = new ArrayList<>();

        for (int slot = 0; slot < numSlots; slot++) {
            List<Double> slotData = slotDemandMap.getOrDefault(slot, new ArrayList<>());

            if (slotData.isEmpty()) {
                predictedDemand.add(calculateDefaultDemandForSlot(slot));
            } else {
                double movingAverage = calculateWeightedMovingAverage(slotData);
                double trend = calculateTrend(slotData);
                predictedDemand.add(Math.max(0, movingAverage + trend));
            }
        }

        return predictedDemand;
    }

    public double[] predictElectricityPrices(int numSlots) {
        double[] prices = new double[numSlots];
        for (int slot = 0; slot < numSlots; slot++) {
            int hour = slot / 4;
            if (hour >= 7 && hour < 10) {
                prices[slot] = 1.2 + 0.3 * Math.sin(slot * 0.1);
            } else if (hour >= 17 && hour < 21) {
                prices[slot] = 1.3 + 0.2 * Math.sin(slot * 0.15);
            } else if (hour >= 0 && hour < 6) {
                prices[slot] = 0.5 + 0.1 * Math.sin(slot * 0.05);
            } else {
                prices[slot] = 0.8 + 0.1 * Math.sin(slot * 0.1);
            }
        }
        return prices;
    }

    public List<ChargeDischargeRecord> generateSampleHistoryData() {
        List<ChargeDischargeRecord> records = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now().minusDays(7);

        for (int day = 0; day < 7; day++) {
            for (int slot = 0; slot < 96; slot++) {
                LocalDateTime time = now.plusDays(day).plusMinutes(slot * 15L);
                double baseDemand = calculateDefaultDemandForSlot(slot);
                double noise = (Math.random() - 0.5) * 20;
                double power = baseDemand + noise;
                double price = predictElectricityPrices(96)[slot];

                ChargeDischargeRecord record = new ChargeDischargeRecord(
                        time, "device-1", power, 0.5 + Math.random() * 0.3, price);
                records.add(record);
            }
        }

        return records;
    }

    private Map<Integer, List<Double>> groupByTimeSlot(List<ChargeDischargeRecord> historicalData) {
        Map<Integer, List<Double>> slotDemandMap = new HashMap<>();

        for (ChargeDischargeRecord record : historicalData) {
            LocalDateTime time = record.getTimestamp();
            int slot = (time.getHour() * 4) + (time.getMinute() / 15);

            slotDemandMap.computeIfAbsent(slot, k -> new ArrayList<>()).add(Math.abs(record.getPower()));
        }

        return slotDemandMap;
    }

    private double calculateWeightedMovingAverage(List<Double> data) {
        if (data.size() <= 1) {
            return data.isEmpty() ? 50.0 : data.get(0);
        }

        double sum = 0;
        double weightSum = 0;
        for (int i = 0; i < data.size(); i++) {
            double weight = i + 1;
            sum += data.get(i) * weight;
            weightSum += weight;
        }

        return sum / weightSum;
    }

    private double calculateTrend(List<Double> data) {
        if (data.size() < 2) {
            return 0;
        }

        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < data.size(); i++) {
            regression.addData(i, data.get(i));
        }

        return regression.getSlope();
    }

    private List<Double> generateDefaultDemand(int numSlots) {
        List<Double> demand = new ArrayList<>();
        for (int slot = 0; slot < numSlots; slot++) {
            demand.add(calculateDefaultDemandForSlot(slot));
        }
        return demand;
    }

    private double calculateDefaultDemandForSlot(int slot) {
        int hour = slot / 4;

        if (hour >= 7 && hour < 10) {
            return 80 + 20 * Math.sin((slot - 28) * 0.2);
        } else if (hour >= 12 && hour < 14) {
            return 70 + 15 * Math.sin((slot - 48) * 0.15);
        } else if (hour >= 17 && hour < 21) {
            return 90 + 25 * Math.sin((slot - 68) * 0.18);
        } else if (hour >= 0 && hour < 6) {
            return 30 + 10 * Math.sin(slot * 0.1);
        } else {
            return 50 + 10 * Math.sin(slot * 0.1);
        }
    }
}
