package com.example.demo.test;

import com.example.demo.entity.ChargingStation;
import com.example.demo.entity.ChargingUser;
import com.example.demo.entity.PowerAllocationResult;
import com.example.demo.service.PowerAllocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Map;

@Slf4j
@Component
public class ChargingStationTest implements CommandLineRunner {

    private final PowerAllocationService powerAllocationService;

    public ChargingStationTest(PowerAllocationService powerAllocationService) {
        this.powerAllocationService = powerAllocationService;
    }

    @Override
    public void run(String... args) {
        log.info("==============================================");
        log.info("      充电站功率分配算法测试开始");
        log.info("==============================================");

        testBasicAllocation();
        testMultipleUsers();
        testUserJoinAndCancel();
        testPowerLimit();

        log.info("==============================================");
        log.info("      充电站功率分配算法测试完成");
        log.info("==============================================");
    }

    private void testBasicAllocation() {
        log.info("\n【测试1】基础功率分配测试");
        log.info("----------------------------------------------");

        ChargingStation station = new ChargingStation("ST001", "充电站一号", 500.0);

        ChargingUser user1 = ChargingUser.builder()
                .userId("U001")
                .userName("张三")
                .startTimeSlot(32)
                .endTimeSlot(48)
                .targetEnergy(50.0)
                .currentEnergy(0.0)
                .maxPower(60.0)
                .active(true)
                .build();

        station.addUser(user1);
        PowerAllocationResult result = powerAllocationService.allocatePower(station);

        printResult(result);
    }

    private void testMultipleUsers() {
        log.info("\n【测试2】多用户功率分配测试");
        log.info("----------------------------------------------");

        ChargingStation station = new ChargingStation("ST002", "充电站二号", 500.0);

        ChargingUser user1 = ChargingUser.builder()
                .userId("U001")
                .userName("张三")
                .startTimeSlot(32)
                .endTimeSlot(48)
                .targetEnergy(40.0)
                .currentEnergy(0.0)
                .maxPower(60.0)
                .active(true)
                .build();

        ChargingUser user2 = ChargingUser.builder()
                .userId("U002")
                .userName("李四")
                .startTimeSlot(36)
                .endTimeSlot(52)
                .targetEnergy(35.0)
                .currentEnergy(0.0)
                .maxPower(60.0)
                .active(true)
                .build();

        ChargingUser user3 = ChargingUser.builder()
                .userId("U003")
                .userName("王五")
                .startTimeSlot(40)
                .endTimeSlot(56)
                .targetEnergy(45.0)
                .currentEnergy(0.0)
                .maxPower(60.0)
                .active(true)
                .build();

        station.addUser(user1);
        station.addUser(user2);
        station.addUser(user3);

        PowerAllocationResult result = powerAllocationService.allocatePower(station);
        printResult(result);
    }

    private void testUserJoinAndCancel() {
        log.info("\n【测试3】用户动态加入和取消测试");
        log.info("----------------------------------------------");

        ChargingStation station = new ChargingStation("ST003", "充电站三号", 500.0);

        ChargingUser user1 = ChargingUser.builder()
                .userId("U001")
                .userName("张三")
                .startTimeSlot(32)
                .endTimeSlot(48)
                .targetEnergy(40.0)
                .currentEnergy(0.0)
                .maxPower(60.0)
                .active(true)
                .build();

        ChargingUser user2 = ChargingUser.builder()
                .userId("U002")
                .userName("李四")
                .startTimeSlot(36)
                .endTimeSlot(52)
                .targetEnergy(35.0)
                .currentEnergy(0.0)
                .maxPower(60.0)
                .active(true)
                .build();

        station.addUser(user1);
        station.addUser(user2);

        log.info("初始分配（2个用户）：");
        PowerAllocationResult result1 = powerAllocationService.allocatePower(station);
        printResult(result1);

        ChargingUser user3 = ChargingUser.builder()
                .userId("U003")
                .userName("王五")
                .startTimeSlot(40)
                .endTimeSlot(56)
                .targetEnergy(30.0)
                .currentEnergy(0.0)
                .maxPower(60.0)
                .active(true)
                .build();

        powerAllocationService.reallocateOnUserJoin(station, user3);
        log.info("用户U003加入后重新分配（3个用户）：");
        PowerAllocationResult result2 = powerAllocationService.allocatePower(station);
        printResult(result2);

        powerAllocationService.reallocateOnUserCancel(station, "U002");
        log.info("用户U002取消后重新分配：");
        PowerAllocationResult result3 = powerAllocationService.allocatePower(station);
        printResult(result3);
    }

    private void testPowerLimit() {
        log.info("\n【测试4】功率限制测试（总功率紧张）");
        log.info("----------------------------------------------");

        ChargingStation station = new ChargingStation("ST004", "充电站四号", 100.0);

        ChargingUser user1 = ChargingUser.builder()
                .userId("U001")
                .userName("张三")
                .startTimeSlot(32)
                .endTimeSlot(40)
                .targetEnergy(30.0)
                .currentEnergy(0.0)
                .maxPower(60.0)
                .active(true)
                .build();

        ChargingUser user2 = ChargingUser.builder()
                .userId("U002")
                .userName("李四")
                .startTimeSlot(32)
                .endTimeSlot(40)
                .targetEnergy(30.0)
                .currentEnergy(0.0)
                .maxPower(60.0)
                .active(true)
                .build();

        ChargingUser user3 = ChargingUser.builder()
                .userId("U003")
                .userName("王五")
                .startTimeSlot(32)
                .endTimeSlot(40)
                .targetEnergy(30.0)
                .currentEnergy(0.0)
                .maxPower(60.0)
                .active(true)
                .build();

        station.addUser(user1);
        station.addUser(user2);
        station.addUser(user3);

        PowerAllocationResult result = powerAllocationService.allocatePower(station);
        printResult(result);
    }

    private void printResult(PowerAllocationResult result) {
        log.info("分配结果: {}", result.getMessage());
        log.info("充电站总功率: {} kW", result.getTotalStationPower());

        log.info("\n用户分配详情：");
        for (PowerAllocationResult.UserAllocationDetail user : result.getUserAllocations()) {
            log.info("  用户: {} ({})", user.getUserName(), user.getUserId());
            log.info("    时间段: {} - {} ({}:{} - {}:{})",
                    user.getStartTimeSlot(), user.getEndTimeSlot(),
                    getTimeString(user.getStartTimeSlot()),
                    getTimeString(user.getEndTimeSlot()));
            log.info("    目标电量: {} kWh, 实际分配: {} kWh, 满足: {}",
                    user.getTargetEnergy(), user.getActualEnergy(), user.isSatisfied());

            StringBuilder powerStr = new StringBuilder("    功率分配: ");
            boolean first = true;
            for (int i = 0; i < 96; i++) {
                if (user.getAllocatedPower()[i] > 0.01) {
                    if (!first) powerStr.append(", ");
                    powerStr.append(String.format("[%d]%s=%.1fkW", i, getTimeString(i), user.getAllocatedPower()[i]));
                    first = false;
                }
            }
            log.info(powerStr.toString());
        }

        log.info("\n时间段功率使用情况：");
        if (result.getTimeSlotPowerMap() != null && !result.getTimeSlotPowerMap().isEmpty()) {
            for (Map.Entry<Integer, Double> entry : result.getTimeSlotPowerMap().entrySet()) {
                int slot = entry.getKey();
                double power = entry.getValue();
                double percentage = (power / result.getTotalStationPower()) * 100;
                log.info("  时段 {} ({}): {:.1f} kW / {:.1f} kW ({:.1f}%)",
                        slot, getTimeString(slot), power, result.getTotalStationPower(), percentage);
            }
        } else {
            log.info("  无功率使用");
        }
        log.info("");
    }

    private String getTimeString(int timeSlot) {
        int totalMinutes = timeSlot * 15;
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }
}
