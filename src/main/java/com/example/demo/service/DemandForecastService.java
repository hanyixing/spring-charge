package com.example.demo.service;

import com.example.demo.model.DemandForecast;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 需求预测服务
 * 基于历史数据预测未来的充放电需求
 */
@Service
public class DemandForecastService {

    // 96个点，每15分钟一个点
    private static final int TIME_POINTS = 96;
    private static final double TIME_INTERVAL_HOURS = 0.25;

    /**
     * 生成未来96个时间点的需求预测
     * 基于历史数据模式进行预测
     */
    public List<DemandForecast> generateForecast(LocalDateTime startTime, 
                                                   List<double[]> historicalData) {
        List<DemandForecast> forecasts = new ArrayList<>();
        
        for (int i = 0; i < TIME_POINTS; i++) {
            LocalDateTime timePoint = startTime.plusMinutes(i * 15);
            DemandForecast forecast = new DemandForecast(i, timePoint);
            
            // 基于历史数据计算预测值
            double[] predictedValues = predictFromHistoricalData(i, historicalData);
            
            forecast.setPredictedChargeDemand(predictedValues[0]);
            forecast.setPredictedDischargeDemand(predictedValues[1]);
            forecast.setPredictedPrice(predictedValues[2]);
            forecast.setConfidence(predictedValues[3]);
            forecast.calculateNetDemand();
            
            forecasts.add(forecast);
        }
        
        return forecasts;
    }
    
    /**
     * 基于历史数据预测
     * 使用加权移动平均算法
     */
    private double[] predictFromHistoricalData(int timeIndex, List<double[]> historicalData) {
        double[] result = new double[4]; // [chargeDemand, dischargeDemand, price, confidence]
        
        if (historicalData == null || historicalData.isEmpty()) {
            // 没有历史数据时使用默认模式
            return generateDefaultPattern(timeIndex);
        }
        
        // 计算加权平均值，越近期的数据权重越高
        double totalWeight = 0;
        double weightedChargeDemand = 0;
        double weightedDischargeDemand = 0;
        double weightedPrice = 0;
        
        int dataSize = historicalData.size();
        for (int i = 0; i < dataSize; i++) {
            double weight = (i + 1.0) / dataSize; // 权重递增
            double[] data = historicalData.get(i);
            
            if (data.length >= 3) {
                weightedChargeDemand += data[0] * weight;
                weightedDischargeDemand += data[1] * weight;
                weightedPrice += data[2] * weight;
                totalWeight += weight;
            }
        }
        
        if (totalWeight > 0) {
            result[0] = weightedChargeDemand / totalWeight;
            result[1] = weightedDischargeDemand / totalWeight;
            result[2] = weightedPrice / totalWeight;
            result[3] = Math.min(0.9, 0.5 + dataSize * 0.05); // 基于数据量计算置信度
        } else {
            return generateDefaultPattern(timeIndex);
        }
        
        return result;
    }
    
    /**
     * 生成默认的日负荷模式
     * 模拟典型的工商业用电模式
     */
    private double[] generateDefaultPattern(int timeIndex) {
        double[] result = new double[4];
        
        // 将96个点映射到24小时
        double hour = timeIndex * 15.0 / 60.0;
        
        // 充电需求模式: 夜间低谷充电
        if (hour >= 23 || hour < 7) {
            result[0] = 500 + Math.random() * 200; // 夜间充电需求高
        } else if (hour >= 10 && hour < 15) {
            result[0] = 200 + Math.random() * 100; // 中午光伏发电时段充电
        } else {
            result[0] = 50 + Math.random() * 50; // 其他时段低充电需求
        }
        
        // 放电需求模式: 峰时段放电
        if ((hour >= 8 && hour < 11) || (hour >= 18 && hour < 22)) {
            result[1] = 800 + Math.random() * 300; // 峰时段放电需求高
        } else if (hour >= 13 && hour < 17) {
            result[1] = 400 + Math.random() * 200; // 平时段中等放电需求
        } else {
            result[1] = 100 + Math.random() * 100; // 其他时段低放电需求
        }
        
        // 电价模式: 峰谷平电价
        if (hour >= 8 && hour < 11) {
            result[2] = 1.2 + Math.random() * 0.2; // 峰时电价
        } else if (hour >= 18 && hour < 22) {
            result[2] = 1.3 + Math.random() * 0.2; // 晚高峰电价
        } else if (hour >= 11 && hour < 18) {
            result[2] = 0.8 + Math.random() * 0.1; // 平时电价
        } else {
            result[2] = 0.3 + Math.random() * 0.1; // 谷时电价
        }
        
        result[3] = 0.7; // 默认置信度
        
        return result;
    }
    
    /**
     * 生成模拟历史数据
     */
    public List<double[]> generateMockHistoricalData(int days) {
        List<double[]> historicalData = new ArrayList<>();
        Random random = new Random();
        
        for (int day = 0; day < days; day++) {
            for (int i = 0; i < TIME_POINTS; i++) {
                double hour = i * 15.0 / 60.0;
                
                double chargeDemand;
                double dischargeDemand;
                double price;
                
                // 充电模式
                if (hour >= 23 || hour < 7) {
                    chargeDemand = 500 + random.nextDouble() * 200;
                } else if (hour >= 10 && hour < 15) {
                    chargeDemand = 200 + random.nextDouble() * 100;
                } else {
                    chargeDemand = 50 + random.nextDouble() * 50;
                }
                
                // 放电模式
                if ((hour >= 8 && hour < 11) || (hour >= 18 && hour < 22)) {
                    dischargeDemand = 800 + random.nextDouble() * 300;
                } else if (hour >= 13 && hour < 17) {
                    dischargeDemand = 400 + random.nextDouble() * 200;
                } else {
                    dischargeDemand = 100 + random.nextDouble() * 100;
                }
                
                // 电价
                if (hour >= 8 && hour < 11) {
                    price = 1.2 + random.nextDouble() * 0.2;
                } else if (hour >= 18 && hour < 22) {
                    price = 1.3 + random.nextDouble() * 0.2;
                } else if (hour >= 11 && hour < 18) {
                    price = 0.8 + random.nextDouble() * 0.1;
                } else {
                    price = 0.3 + random.nextDouble() * 0.1;
                }
                
                historicalData.add(new double[]{chargeDemand, dischargeDemand, price});
            }
        }
        
        return historicalData;
    }
    
    /**
     * 更新预测模型
     */
    public void updateForecastModel(List<double[]> newData) {
        // 这里可以实现更复杂的机器学习模型更新
        // 例如: 时间序列分析、神经网络等
    }
}
