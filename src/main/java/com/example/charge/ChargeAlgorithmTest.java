package com.example.charge;

import com.example.charge.model.ChargeRequest;
import com.example.charge.model.ChargeResult;
import com.example.charge.model.ElectricityPrice;
import com.example.charge.service.ChargeOptimizationService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ChargeAlgorithmTest {

    public static void main(String[] args) {
        ChargeOptimizationService service = new ChargeOptimizationService();
        List<ElectricityPrice> defaultPrices = service.generateDefaultPriceList();

        System.out.println("========== 用户充电省钱算法测试 ==========");
        System.out.println();

        ChargeRequest request = new ChargeRequest();
        request.setCurrentBattery(20);
        request.setTargetBattery(80);
        request.setStartTimePoint(32);
        request.setEndTimePoint(80);
        request.setBatteryCapacity(new BigDecimal("70"));
        request.setMaxChargePower(new BigDecimal("7"));
        request.setPriceList(defaultPrices);

        System.out.println("输入参数：");
        System.out.println("- 当前电量: " + request.getCurrentBattery() + "%");
        System.out.println("- 目标电量: " + request.getTargetBattery() + "%");
        System.out.println("- 充电开始时间点: " + request.getStartTimePoint() + " (" + formatTime(request.getStartTimePoint()) + ")");
        System.out.println("- 充电结束时间点: " + request.getEndTimePoint() + " (" + formatTime(request.getEndTimePoint()) + ")");
        System.out.println("- 电池容量: " + request.getBatteryCapacity() + " kWh");
        System.out.println("- 最大充电功率: " + request.getMaxChargePower() + " kW");
        System.out.println();

        ChargeResult result = service.calculateOptimalCharge(request);

        System.out.println("计算结果：");
        System.out.println("- 计算状态: " + (result.isSuccess() ? "成功" : "失败"));
        System.out.println("- 消息: " + result.getMessage());
        if (result.isSuccess()) {
            System.out.println("- 总充电量: " + result.getTotalEnergy() + " kWh");
            System.out.println("- 最低成本: " + result.getTotalCost() + " 元");
            System.out.println();
            System.out.println("充电调度计划:");
            for (String schedule : result.getChargeSchedule()) {
                System.out.println("  " + schedule);
            }
            System.out.println();
            System.out.println("各时间点充电功率详情:");
            Map<Integer, BigDecimal> powerMap = result.getPowerPerPoint();
            for (int i = request.getStartTimePoint(); i < request.getEndTimePoint(); i++) {
                BigDecimal power = powerMap.get(i);
                if (power.compareTo(BigDecimal.ZERO) > 0) {
                    System.out.printf("  时间点%02d (%s): %.2f kW\n", i, formatTime(i), power);
                }
            }
        }

        System.out.println();
        System.out.println("========== 测试完成 ==========");
    }

    private static String formatTime(int point) {
        int totalMinutes = point * 15;
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }
}
