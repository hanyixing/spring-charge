package com.example.demo.service;

import com.example.demo.model.ChargingCurve;
import com.example.demo.model.ElectricityPrice;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 模拟数据服务
 * 用于生成测试数据
 */
@Service
public class MockDataService {
    private static final Logger log = LoggerFactory.getLogger(MockDataService.class);
    
    private static final int TIME_SLOTS = 96;
    private static final double SLOT_DURATION = 0.25;
    private static final Random random = new Random(42); // 固定种子以便复现
    
    /**
     * 生成用户历史充电曲线数据
     * 
     * @param userId 用户ID
     * @param days 生成多少天的历史数据
     * @return 历史充电曲线列表
     */
    public List<ChargingCurve> generateHistoryCurves(String userId, int days) {
        List<ChargingCurve> curves = new ArrayList<>();
        
        // 模拟用户习惯：晚上22:00左右开始充电，持续6-8小时
        int preferredStartSlot = 88; // 22:00
        double avgChargingPower = 6.0; // 平均6kW
        double targetEnergy = 40.0; // 目标充电40kWh
        
        for (int i = 0; i < days; i++) {
            LocalDate date = LocalDate.now().minusDays(days - i);
            
            // 添加一些随机性，模拟真实用户的习惯波动
            int startSlot = preferredStartSlot + random.nextInt(8) - 4; // ±1小时波动
            startSlot = Math.max(0, Math.min(TIME_SLOTS - 1, startSlot));
            
            double duration = 6 + random.nextDouble() * 2; // 6-8小时
            int durationSlots = (int) (duration / SLOT_DURATION);
            
            double power = avgChargingPower + random.nextDouble() * 1 - 0.5; // 功率波动
            power = Math.max(3.0, Math.min(7.0, power));
            
            double[] powerData = new double[TIME_SLOTS];
            double totalEnergy = 0;
            double maxPower = 0;
            
            for (int j = 0; j < durationSlots && (startSlot + j) < TIME_SLOTS; j++) {
                int slot = (startSlot + j) % TIME_SLOTS;
                // 模拟充电曲线：开始和结束时有功率 ramp up/down
                double factor = 1.0;
                if (j < 2) factor = (j + 1) / 3.0; // 开始 ramp up
                if (j > durationSlots - 3) factor = (durationSlots - j) / 3.0; // 结束 ramp down
                
                powerData[slot] = power * factor * (0.9 + random.nextDouble() * 0.2);
                totalEnergy += powerData[slot] * SLOT_DURATION;
                maxPower = Math.max(maxPower, powerData[slot]);
            }
            
            curves.add(ChargingCurve.builder()
                    .chargingId(UUID.randomUUID().toString())
                    .userId(userId)
                    .chargingDate(date)
                    .powerData(powerData)
                    .totalEnergy(totalEnergy)
                    .startTimeSlot(startSlot)
                    .endTimeSlot((startSlot + durationSlots) % TIME_SLOTS)
                    .chargingDuration(duration)
                    .avgPower(totalEnergy / duration)
                    .maxPower(maxPower)
                    .build());
        }
        
        log.info("为用户[{}]生成了{}条历史充电记录", userId, curves.size());
        return curves;
    }
    
    /**
     * 生成特定用户的不同习惯数据
     * 
     * @param userId 用户ID
     * @param pattern 习惯模式：NIGHT-夜间充电, DAY-白天充电, MIXED-混合
     * @param days 天数
     * @return 历史充电曲线列表
     */
    public List<ChargingCurve> generateHistoryCurvesWithPattern(String userId, String pattern, int days) {
        List<ChargingCurve> curves = new ArrayList<>();
        
        int baseStartSlot;
        switch (pattern.toUpperCase()) {
            case "DAY":
                baseStartSlot = 36; // 09:00
                break;
            case "MIXED":
                baseStartSlot = random.nextBoolean() ? 88 : 36; // 随机夜间或白天
                break;
            case "NIGHT":
            default:
                baseStartSlot = 88; // 22:00
                break;
        }
        
        for (int i = 0; i < days; i++) {
            LocalDate date = LocalDate.now().minusDays(days - i);
            
            int startSlot = baseStartSlot + random.nextInt(8) - 4;
            startSlot = Math.max(0, Math.min(TIME_SLOTS - 1, startSlot));
            
            double duration = 5 + random.nextDouble() * 3;
            int durationSlots = (int) (duration / SLOT_DURATION);
            
            double power = 5.0 + random.nextDouble() * 2;
            
            double[] powerData = new double[TIME_SLOTS];
            double totalEnergy = 0;
            double maxPower = 0;
            
            for (int j = 0; j < durationSlots && (startSlot + j) < TIME_SLOTS; j++) {
                int slot = (startSlot + j) % TIME_SLOTS;
                double factor = 1.0;
                if (j < 2) factor = (j + 1) / 3.0;
                if (j > durationSlots - 3) factor = (durationSlots - j) / 3.0;
                
                powerData[slot] = power * factor;
                totalEnergy += powerData[slot] * SLOT_DURATION;
                maxPower = Math.max(maxPower, powerData[slot]);
            }
            
            curves.add(ChargingCurve.builder()
                    .chargingId(UUID.randomUUID().toString())
                    .userId(userId)
                    .chargingDate(date)
                    .powerData(powerData)
                    .totalEnergy(totalEnergy)
                    .startTimeSlot(startSlot)
                    .endTimeSlot((startSlot + durationSlots) % TIME_SLOTS)
                    .chargingDuration(duration)
                    .avgPower(totalEnergy / duration)
                    .maxPower(maxPower)
                    .build());
        }
        
        return curves;
    }
    
    /**
     * 获取指定日期的电价信息
     */
    public ElectricityPrice getElectricityPrice(LocalDate date) {
        return ElectricityPrice.createDefaultPrice(date);
    }
}
