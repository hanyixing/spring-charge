package com.example.demo.service;

import com.example.demo.model.EnergyStorageDevice;
import com.example.demo.model.OptimizationResult;
import com.example.demo.model.TimeSlotPower;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EnergyOptimizationService {

    @Autowired
    private DemandPredictionService demandPredictionService;

    public OptimizationResult optimizeChargeDischarge(List<EnergyStorageDevice> devices) {
        int numSlots = 96;
        double[] prices = demandPredictionService.predictElectricityPrices(numSlots);
        List<Double> predictedDemand = demandPredictionService.predictFutureDemand(
                demandPredictionService.generateSampleHistoryData(), numSlots);

        List<TimeSlotPower> timeSlotPowers = initializeTimeSlots(numSlots, prices, predictedDemand);
        Map<String, List<Double>> devicePowerPerSlot = initializeDevicePower(devices, numSlots);
        Map<String, Double> deviceSOC = initializeSOC(devices);
        Map<String, List<Double>> deviceSOCHistory = new HashMap<>();
        for (EnergyStorageDevice device : devices) {
            deviceSOCHistory.put(device.getId(), new ArrayList<>());
            deviceSOCHistory.get(device.getId()).add(device.getCurrentSOC());
        }

        double totalCost = 0;
        boolean demandSatisfied = true;
        boolean socWithinLimits = true;

        for (int slot = 0; slot < numSlots; slot++) {
            double demand = predictedDemand.get(slot);
            double price = prices[slot];

            OptimizationSlotResult result = optimizeSlot(
                    slot, devices, demand, price, deviceSOC, devicePowerPerSlot);

            timeSlotPowers.get(slot).setChargePower(result.totalCharge);
            timeSlotPowers.get(slot).setDischargePower(result.totalDischarge);
            timeSlotPowers.get(slot).setDemand(demand);
            totalCost += result.slotCost;

            if (!result.demandMet) {
                demandSatisfied = false;
            }

            for (EnergyStorageDevice device : devices) {
                double currentSOC = deviceSOC.get(device.getId());
                deviceSOCHistory.get(device.getId()).add(currentSOC);
                if (currentSOC > 0.95 || currentSOC < 0.05) {
                    socWithinLimits = false;
                }
            }
        }

        OptimizationResult result = new OptimizationResult();
        result.setTotalCost(Math.round(totalCost * 100.0) / 100.0);
        result.setTimeSlotPowers(timeSlotPowers);
        result.setDevicePowerPerSlot(devicePowerPerSlot);
        result.setDeviceSOCHistory(deviceSOCHistory);
        result.setDemandSatisfied(demandSatisfied);
        result.setSocWithinLimits(socWithinLimits);
        result.setMessage(demandSatisfied && socWithinLimits ? "优化成功，所有约束已满足" :
                (!demandSatisfied ? "警告：部分时段放电需求未能完全满足" : "警告：SOC超出安全范围"));

        return result;
    }

    private List<TimeSlotPower> initializeTimeSlots(int numSlots, double[] prices, List<Double> demand) {
        List<TimeSlotPower> slots = new ArrayList<>();
        for (int i = 0; i < numSlots; i++) {
            TimeSlotPower slot = new TimeSlotPower(i, prices[i]);
            slot.setDemand(demand.get(i));
            slots.add(slot);
        }
        return slots;
    }

    private Map<String, List<Double>> initializeDevicePower(List<EnergyStorageDevice> devices, int numSlots) {
        Map<String, List<Double>> powerMap = new HashMap<>();
        for (EnergyStorageDevice device : devices) {
            List<Double> powers = new ArrayList<>(Collections.nCopies(numSlots, 0.0));
            powerMap.put(device.getId(), powers);
        }
        return powerMap;
    }

    private Map<String, Double> initializeSOC(List<EnergyStorageDevice> devices) {
        Map<String, Double> socMap = new HashMap<>();
        for (EnergyStorageDevice device : devices) {
            socMap.put(device.getId(), device.getCurrentSOC());
        }
        return socMap;
    }

    private OptimizationSlotResult optimizeSlot(
            int slot,
            List<EnergyStorageDevice> devices,
            double demand,
            double price,
            Map<String, Double> deviceSOC,
            Map<String, List<Double>> devicePowerPerSlot) {

        double totalDischargeNeeded = demand;
        double totalDischarge = 0;
        double totalCharge = 0;
        double slotCost = 0;
        boolean demandMet = true;

        List<DeviceWithCost> dischargeDevices = new ArrayList<>();
        List<DeviceWithCost> chargeDevices = new ArrayList<>();

        for (EnergyStorageDevice device : devices) {
            double currentSOC = deviceSOC.get(device.getId());
            double dischargeCost = calculateDischargeCost(device, currentSOC, price);
            double chargeCost = calculateChargeCost(device, currentSOC, price);

            if (canDischarge(device, currentSOC)) {
                dischargeDevices.add(new DeviceWithCost(device, dischargeCost));
            }
            if (canCharge(device, currentSOC)) {
                chargeDevices.add(new DeviceWithCost(device, chargeCost));
            }
        }

        Collections.sort(dischargeDevices);
        Collections.sort(chargeDevices);

        for (DeviceWithCost dwc : dischargeDevices) {
            if (totalDischarge >= totalDischargeNeeded) {
                break;
            }
            EnergyStorageDevice device = dwc.device;
            double currentSOC = deviceSOC.get(device.getId());
            double maxPossibleDischarge = calculateMaxDischarge(device, currentSOC);
            double neededDischarge = totalDischargeNeeded - totalDischarge;
            double actualDischarge = Math.min(maxPossibleDischarge, neededDischarge);
            actualDischarge = Math.max(actualDischarge, device.getMinDischargePower());

            if (actualDischarge > 0) {
                devicePowerPerSlot.get(device.getId()).set(slot, -actualDischarge);
                totalDischarge += actualDischarge;

                double energy = actualDischarge * 0.25 / device.getDischargeEfficiency();
                double newSOC = currentSOC - energy / device.getMaxCapacity();
                deviceSOC.put(device.getId(), Math.max(0.1, newSOC));
            }
        }

        if (totalDischarge < totalDischargeNeeded * 0.95) {
            demandMet = false;
        }

        for (DeviceWithCost dwc : chargeDevices) {
            EnergyStorageDevice device = dwc.device;
            double currentSOC = deviceSOC.get(device.getId());
            double maxPossibleCharge = calculateMaxCharge(device, currentSOC);

            boolean emergencyCharge = currentSOC < 0.2;
            boolean lowSOCCharge = currentSOC < 0.3;
            boolean economicCharge = price < 0.7;

            double actualCharge = 0;
            if (emergencyCharge) {
                actualCharge = Math.min(maxPossibleCharge, device.getMaxChargePower());
            } else if (lowSOCCharge) {
                actualCharge = Math.min(maxPossibleCharge * 0.7, device.getMaxChargePower() * 0.5);
            } else if (economicCharge) {
                actualCharge = maxPossibleCharge;
            }

            if (actualCharge > device.getMinChargePower()) {
                devicePowerPerSlot.get(device.getId()).set(slot, actualCharge);
                totalCharge += actualCharge;
                slotCost += actualCharge * price * 0.25;

                double energy = actualCharge * 0.25 * device.getChargeEfficiency();
                double newSOC = currentSOC + energy / device.getMaxCapacity();
                deviceSOC.put(device.getId(), Math.min(0.9, newSOC));
            }
        }

        return new OptimizationSlotResult(totalCharge, totalDischarge, slotCost, demandMet);
    }

    private double calculateDischargeCost(EnergyStorageDevice device, double soc, double price) {
        double socFactor = 1.0 - soc;
        double efficiencyFactor = 1.0 / device.getDischargeEfficiency();
        return socFactor * 10 + efficiencyFactor * 5 + price * 2;
    }

    private double calculateChargeCost(EnergyStorageDevice device, double soc, double price) {
        double socFactor = soc;
        double efficiencyFactor = 1.0 / device.getChargeEfficiency();
        return price * 10 + socFactor * 5 + efficiencyFactor * 2;
    }

    private boolean canDischarge(EnergyStorageDevice device, double soc) {
        return soc > 0.25;
    }

    private boolean canCharge(EnergyStorageDevice device, double soc) {
        return soc < 0.85;
    }

    private double calculateMaxDischarge(EnergyStorageDevice device, double soc) {
        double powerLimit = device.getMaxDischargePower();
        double emergencyReserve = 0.2;
        double socLimit = (soc - emergencyReserve) * device.getMaxCapacity() * 4 * device.getDischargeEfficiency();
        return Math.min(powerLimit, Math.max(0, socLimit));
    }

    private double calculateMaxCharge(EnergyStorageDevice device, double soc) {
        double powerLimit = device.getMaxChargePower();
        double socLimit = (0.9 - soc) * device.getMaxCapacity() * 4 / device.getChargeEfficiency();
        return Math.min(powerLimit, Math.max(0, socLimit));
    }

    public List<EnergyStorageDevice> createSampleDevices() {
        List<EnergyStorageDevice> devices = new ArrayList<>();
        devices.add(new EnergyStorageDevice(
                "device-1", "储能设备A",
                100, 10,
                80, 5,
                500
        ));
        devices.add(new EnergyStorageDevice(
                "device-2", "储能设备B",
                150, 15,
                120, 10,
                750
        ));
        devices.add(new EnergyStorageDevice(
                "device-3", "储能设备C",
                80, 8,
                60, 5,
                400
        ));
        return devices;
    }

    private static class DeviceWithCost implements Comparable<DeviceWithCost> {
        EnergyStorageDevice device;
        double cost;

        DeviceWithCost(EnergyStorageDevice device, double cost) {
            this.device = device;
            this.cost = cost;
        }

        @Override
        public int compareTo(DeviceWithCost other) {
            return Double.compare(this.cost, other.cost);
        }
    }

    private static class OptimizationSlotResult {
        double totalCharge;
        double totalDischarge;
        double slotCost;
        boolean demandMet;

        OptimizationSlotResult(double totalCharge, double totalDischarge, double slotCost, boolean demandMet) {
            this.totalCharge = totalCharge;
            this.totalDischarge = totalDischarge;
            this.slotCost = slotCost;
            this.demandMet = demandMet;
        }
    }
}
