package com.example.demo.service;

import com.example.demo.config.RewardConfig;
import com.example.demo.model.ChargeRewardResult;
import com.example.demo.model.ChargeSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ChargeRewardService {

    @Autowired
    private RewardConfig rewardConfig;

    private static final int SLOTS_PER_DAY = 96;
    private static final int MINUTES_PER_SLOT = 15;

    public ChargeRewardResult calculateReward(ChargeSession session) {
        initRewardSlots();

        ChargeRewardResult result = new ChargeRewardResult();
        result.setUserId(session.getUserId());

        List<LocalDateTime> chargeTimeSlots = getTimeSlots(session.getStartTime(), session.getEndTime());
        BigDecimal totalEnergy = session.getEnergyKwh();
        BigDecimal energyPerSlot = totalEnergy.divide(
                BigDecimal.valueOf(chargeTimeSlots.size()), 4, RoundingMode.HALF_UP);

        List<ChargeRewardResult.TimeSlotReward> slotRewards = new ArrayList<>();
        BigDecimal totalReward = BigDecimal.ZERO;
        BigDecimal rewardedEnergy = BigDecimal.ZERO;
        BigDecimal nonRewardedEnergy = BigDecimal.ZERO;

        for (LocalDateTime slotTime : chargeTimeSlots) {
            ChargeRewardResult.TimeSlotReward slotReward = new ChargeRewardResult.TimeSlotReward();
            int slotIndex = calculateTimeSlotIndex(slotTime.toLocalTime());
            
            slotReward.setDate(slotTime.toLocalDate().toString());
            slotReward.setTimeSlotIndex(slotIndex);
            slotReward.setTimeRange(getTimeRangeForSlot(slotIndex));
            slotReward.setEnergy(energyPerSlot);

            boolean isRewardPeriod = rewardConfig.getRewardTimeSlots().contains(slotIndex);
            slotReward.setRewardPeriod(isRewardPeriod);

            if (isRewardPeriod) {
                BigDecimal slotRewardAmount = energyPerSlot.multiply(rewardConfig.getRewardRatePerKwh())
                        .setScale(2, RoundingMode.HALF_UP);
                slotReward.setReward(slotRewardAmount);
                totalReward = totalReward.add(slotRewardAmount);
                rewardedEnergy = rewardedEnergy.add(energyPerSlot);
            } else {
                slotReward.setReward(BigDecimal.ZERO);
                nonRewardedEnergy = nonRewardedEnergy.add(energyPerSlot);
            }

            slotRewards.add(slotReward);
        }

        result.setTotalReward(totalReward.setScale(2, RoundingMode.HALF_UP));
        result.setRewardedEnergy(rewardedEnergy.setScale(2, RoundingMode.HALF_UP));
        result.setNonRewardedEnergy(nonRewardedEnergy.setScale(2, RoundingMode.HALF_UP));
        result.setTimeSlotRewards(slotRewards);

        return result;
    }

    private void initRewardSlots() {
        if (rewardConfig.getRewardTimeSlots() == null || rewardConfig.getRewardTimeSlots().isEmpty()) {
            rewardConfig.setRewardTimeSlots(IntStream.range(0, 24)
                    .boxed().collect(Collectors.toList()));
        }
    }

    private List<LocalDateTime> getTimeSlots(LocalDateTime startTime, LocalDateTime endTime) {
        List<LocalDateTime> slots = new ArrayList<>();
        LocalDateTime current = LocalDateTime.of(
                startTime.toLocalDate(),
                LocalTime.of(startTime.getHour(), (startTime.getMinute() / MINUTES_PER_SLOT) * MINUTES_PER_SLOT)
        );
        
        while (!current.isAfter(endTime)) {
            slots.add(current);
            current = current.plusMinutes(MINUTES_PER_SLOT);
        }

        return slots;
    }

    private int calculateTimeSlotIndex(LocalTime time) {
        int minutes = time.getHour() * 60 + time.getMinute();
        return minutes / MINUTES_PER_SLOT;
    }

    private String getTimeRangeForSlot(int slotIndex) {
        int startMinutes = slotIndex * MINUTES_PER_SLOT;
        int endMinutes = startMinutes + MINUTES_PER_SLOT;

        LocalTime startTime = LocalTime.ofSecondOfDay(startMinutes * 60L);
        LocalTime endTime = LocalTime.ofSecondOfDay(Math.min(endMinutes * 60L, 24 * 3600 - 1));

        return String.format("%02d:%02d-%02d:%02d",
                startTime.getHour(), startTime.getMinute(),
                endTime.getHour(), endTime.getMinute());
    }

    public List<String> getAllRewardPeriods() {
        initRewardSlots();
        return rewardConfig.getRewardTimeSlots().stream()
                .map(this::getTimeRangeForSlot)
                .collect(Collectors.toList());
    }
}
