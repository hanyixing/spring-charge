package com.example.demo;

import com.example.demo.model.*;
import com.example.demo.service.ChargeDischargeOptimizationService;
import com.example.demo.service.DemandForecastService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 储能设备集群充放电算法测试
 */
@SpringBootTest
public class EnergyStorageOptimizationTest {

    @Autowired
    private DemandForecastService forecastService;

    @Autowired
    private ChargeDischargeOptimizationService optimizationService;

    private String repeatChar(char c, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    @Test
    public void testOptimization() {
        System.out.println("\n" + repeatChar('=', 80));
        System.out.println("储能设备集群充放电优化算法测试");
        System.out.println(repeatChar('=', 80) + "\n");

        // 1. 创建设备集群
        System.out.println("【步骤1】创建设备集群...");
        List<StorageDevice> devices = createStorageDevices(5);
        printDeviceInfo(devices);

        // 2. 生成历史数据
        System.out.println("\n【步骤2】生成历史数据（模拟7天数据）...");
        List<double[]> historicalData = forecastService.generateMockHistoricalData(7);
        System.out.println("✓ 已生成 " + historicalData.size() + " 条历史数据记录");

        // 3. 生成需求预测
        System.out.println("\n【步骤3】生成未来96点需求预测...");
        LocalDateTime startTime = LocalDateTime.now();
        List<DemandForecast> forecasts = forecastService.generateForecast(startTime, historicalData);
        printForecastSummary(forecasts);

        // 4. 执行优化
        System.out.println("\n【步骤4】执行充放电优化调度...");
        long startOptimization = System.currentTimeMillis();
        OptimizationResult result = optimizationService.optimize(devices, startTime, historicalData);
        long optimizationTime = System.currentTimeMillis() - startOptimization;

        // 5. 输出结果
        System.out.println("\n" + repeatChar('=', 80));
        System.out.println("优化结果汇总");
        System.out.println(repeatChar('=', 80));
        printOptimizationResult(result, optimizationTime);

        // 6. 输出详细调度计划
        System.out.println("\n" + repeatChar('=', 80));
        System.out.println("详细调度计划（每小时显示一次）");
        System.out.println(repeatChar('=', 80));
        printScheduleDetails(result.getTotalSchedule());

        // 7. 验证约束
        System.out.println("\n" + repeatChar('=', 80));
        System.out.println("约束验证结果");
        System.out.println(repeatChar('=', 80));
        printConstraintValidation(result);

        System.out.println("\n" + repeatChar('=', 80));
        System.out.println("测试完成！");
        System.out.println(repeatChar('=', 80) + "\n");
    }

    /**
     * 创建设备集群
     */
    private List<StorageDevice> createStorageDevices(int count) {
        List<StorageDevice> devices = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            StorageDevice device = new StorageDevice();
            device.setDeviceId("DEVICE_" + String.format("%03d", i + 1));
            device.setDeviceName("储能设备 " + (i + 1));

            double capacity = 500 + random.nextDouble() * 500;
            device.setRatedCapacity(capacity);
            device.setMaxChargePower(capacity * 0.5);
            device.setMaxDischargePower(capacity * 0.5);
            device.setMinChargePower(capacity * 0.05);
            device.setMinDischargePower(capacity * 0.05);
            device.setCurrentSoc(0.3 + random.nextDouble() * 0.4);
            device.setMaxSoc(0.95);
            device.setMinSoc(0.1);
            device.setChargeEfficiency(0.95);
            device.setDischargeEfficiency(0.95);
            device.setStatus(StorageDevice.DeviceStatus.IDLE);

            devices.add(device);
        }

        return devices;
    }

    /**
     * 打印设备信息
     */
    private void printDeviceInfo(List<StorageDevice> devices) {
        double totalCapacity = 0;
        double totalMaxCharge = 0;
        double totalMaxDischarge = 0;
        double totalSoc = 0;

        for (StorageDevice device : devices) {
            totalCapacity += device.getRatedCapacity();
            totalMaxCharge += device.getMaxChargePower();
            totalMaxDischarge += device.getMaxDischargePower();
            totalSoc += device.getCurrentSoc();
        }
        double avgSoc = totalSoc / devices.size();

        System.out.println("✓ 已创建 " + devices.size() + " 台储能设备");
        System.out.printf("  - 总容量: %.2f kWh%n", totalCapacity);
        System.out.printf("  - 总最大充电功率: %.2f kW%n", totalMaxCharge);
        System.out.printf("  - 总最大放电功率: %.2f kW%n", totalMaxDischarge);
        System.out.printf("  - 平均初始SOC: %.2f%%%n", avgSoc * 100);

        System.out.println("\n设备详情:");
        System.out.println(repeatChar('-', 80));
        System.out.printf("%-12s %-15s %-12s %-12s %-12s %-12s%n",
                "设备ID", "名称", "容量(kWh)", "最大充电(kW)", "最大放电(kW)", "初始SOC(%)");
        System.out.println(repeatChar('-', 80));

        for (StorageDevice device : devices) {
            System.out.printf("%-12s %-15s %12.2f %12.2f %12.2f %12.2f%n",
                    device.getDeviceId(),
                    device.getDeviceName(),
                    device.getRatedCapacity(),
                    device.getMaxChargePower(),
                    device.getMaxDischargePower(),
                    device.getCurrentSoc() * 100);
        }
    }

    /**
     * 打印预测摘要
     */
    private void printForecastSummary(List<DemandForecast> forecasts) {
        double totalChargeDemand = 0;
        double totalDischargeDemand = 0;
        double totalPrice = 0;

        for (DemandForecast forecast : forecasts) {
            totalChargeDemand += forecast.getPredictedChargeDemand();
            totalDischargeDemand += forecast.getPredictedDischargeDemand();
            totalPrice += forecast.getPredictedPrice();
        }

        double avgChargeDemand = totalChargeDemand / forecasts.size();
        double avgDischargeDemand = totalDischargeDemand / forecasts.size();
        double avgPrice = totalPrice / forecasts.size();

        System.out.println("✓ 已生成96点预测数据");
        System.out.printf("  - 平均充电需求: %.2f kW%n", avgChargeDemand);
        System.out.printf("  - 平均放电需求: %.2f kW%n", avgDischargeDemand);
        System.out.printf("  - 平均电价: %.2f 元/kWh%n", avgPrice);
    }

    /**
     * 打印优化结果
     */
    private void printOptimizationResult(OptimizationResult result, long optimizationTime) {
        System.out.printf("优化耗时: %d ms%n", optimizationTime);
        System.out.println(repeatChar('-', 80));
        System.out.printf("总充电量:     %10.2f kWh%n", result.getTotalChargeEnergy());
        System.out.printf("总放电量:     %10.2f kWh%n", result.getTotalDischargeEnergy());
        System.out.printf("总成本:       %10.2f 元%n", result.getTotalCost());
        System.out.printf("总收益:       %10.2f 元%n", result.getTotalRevenue());
        System.out.printf("净利润:       %10.2f 元%n", result.getNetProfit());
        System.out.println(repeatChar('-', 80));
        System.out.printf("初始SOC:      %10.2f%%%n", result.getInitialTotalSoc() * 100);
        System.out.printf("最终SOC:      %10.2f%%%n", result.getFinalTotalSoc() * 100);
        System.out.printf("SOC变化:      %+.2f%%%n", result.getSocChange() * 100);
    }

    /**
     * 打印调度详情
     */
    private void printScheduleDetails(List<PowerSchedule> schedules) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        System.out.printf("%-8s %-8s %-12s %-12s %-12s %-10s %-10s %-12s%n",
                "时间点", "时间", "充电(kW)", "放电(kW)", "净功率(kW)", "电价", "SOC(%)", "成本(元)");
        System.out.println(repeatChar('-', 80));

        for (PowerSchedule schedule : schedules) {
            if (schedule.getTimeIndex() % 4 == 0) { // 每小时显示一次
                System.out.printf("%-8d %-8s %12.2f %12.2f %12.2f %10.2f %10.2f %12.2f%n",
                        schedule.getTimeIndex(),
                        schedule.getTimePoint().format(timeFormatter),
                        schedule.getChargePower(),
                        schedule.getDischargePower(),
                        schedule.getNetPower(),
                        schedule.getElectricityPrice(),
                        schedule.getCumulativeSoc() * 100,
                        schedule.getEstimatedCost());
            }
        }
    }

    /**
     * 打印约束验证结果
     */
    private void printConstraintValidation(OptimizationResult result) {
        if (result.isConstraintsSatisfied()) {
            System.out.println("✓ 所有约束条件均满足");
        } else {
            System.out.println("✗ 存在约束违反:");
            for (String violation : result.getConstraintViolations()) {
                System.out.println("  - " + violation);
            }
        }
    }
}
