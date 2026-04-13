package com.example.demo.service;

import com.example.demo.dto.ChargingRewardResponse;
import com.example.demo.enums.TimeSlotType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChargingRewardService {

    private static final int TOTAL_POINTS = 96;
    private static final int VALLEY_START_1 = 0;
    private static final int VALLEY_END_1 = 28;
    private static final int VALLEY_START_2 = 92;
    private static final int VALLEY_END_2 = 96;
    private static final int[] PEAK_STARTS = {32, 68};
    private static final int[] PEAK_ENDS = {48, 84};

    public TimeSlotType getTimeSlotType(int point) {
        validatePoint(point);

        if ((point >= VALLEY_START_1 && point < VALLEY_END_1) ||
            (point >= VALLEY_START_2 && point < VALLEY_END_2)) {
            return TimeSlotType.VALLEY;
        }

        for (int i = 0; i < PEAK_STARTS.length; i++) {
            if (point >= PEAK_STARTS[i] && point < PEAK_ENDS[i]) {
                return TimeSlotType.PEAK;
            }
        }

        return TimeSlotType.NORMAL;
    }

    public boolean isRewardPeriod(int point) {
        return getTimeSlotType(point) == TimeSlotType.VALLEY;
    }

    public ChargingRewardResponse calculateReward(Integer startPoint, Integer endPoint,
                                                   Double chargingKwh, Double rewardPerKwh) {
        return calculateReward(startPoint, endPoint, chargingKwh, rewardPerKwh, null);
    }

    public ChargingRewardResponse calculateReward(Integer startPoint, Integer endPoint,
                                                   Double chargingKwh, Double rewardPerKwh,
                                                   Integer days) {
        validateStartPoint(startPoint);
        validateEndPoint(endPoint);
        if (chargingKwh == null || chargingKwh <= 0) {
            throw new IllegalArgumentException("充电量必须大于0");
        }
        if (rewardPerKwh == null || rewardPerKwh < 0) {
            throw new IllegalArgumentException("奖励单价不能为负数");
        }

        int effectiveDays = (days == null || days < 1) ? 1 : days;

        boolean isCrossDay = effectiveDays > 1 || endPoint <= startPoint;

        int totalPoints;
        int rewardPoints = 0;
        List<ChargingRewardResponse.TimePointDetail> details = new ArrayList<>();

        if (effectiveDays == 1) {
            if (endPoint <= startPoint) {
                totalPoints = (TOTAL_POINTS - startPoint) + endPoint;

                for (int point = startPoint; point < TOTAL_POINTS; point++) {
                    boolean isReward = isRewardPeriod(point);
                    if (isReward) rewardPoints++;
                    details.add(buildTimePointDetail(point, 0, isReward));
                }
                for (int point = 0; point < endPoint; point++) {
                    boolean isReward = isRewardPeriod(point);
                    if (isReward) rewardPoints++;
                    details.add(buildTimePointDetail(point, 1, isReward));
                }
            } else {
                totalPoints = endPoint - startPoint;

                for (int point = startPoint; point < endPoint; point++) {
                    boolean isReward = isRewardPeriod(point);
                    if (isReward) rewardPoints++;
                    details.add(buildTimePointDetail(point, 0, isReward));
                }
            }
        } else {
            if (endPoint <= startPoint) {
                totalPoints = (effectiveDays - 1) * TOTAL_POINTS + (TOTAL_POINTS - startPoint) + endPoint;

                for (int point = startPoint; point < TOTAL_POINTS; point++) {
                    boolean isReward = isRewardPeriod(point);
                    if (isReward) rewardPoints++;
                    details.add(buildTimePointDetail(point, 0, isReward));
                }
                for (int day = 1; day < effectiveDays; day++) {
                    for (int point = 0; point < TOTAL_POINTS; point++) {
                        boolean isReward = isRewardPeriod(point);
                        if (isReward) rewardPoints++;
                        details.add(buildTimePointDetail(point, day, isReward));
                    }
                }
                for (int point = 0; point < endPoint; point++) {
                    boolean isReward = isRewardPeriod(point);
                    if (isReward) rewardPoints++;
                    details.add(buildTimePointDetail(point, effectiveDays, isReward));
                }
            } else {
                totalPoints = (effectiveDays - 1) * TOTAL_POINTS + (endPoint - startPoint);

                for (int point = startPoint; point < TOTAL_POINTS; point++) {
                    boolean isReward = isRewardPeriod(point);
                    if (isReward) rewardPoints++;
                    details.add(buildTimePointDetail(point, 0, isReward));
                }
                for (int day = 1; day < effectiveDays - 1; day++) {
                    for (int point = 0; point < TOTAL_POINTS; point++) {
                        boolean isReward = isRewardPeriod(point);
                        if (isReward) rewardPoints++;
                        details.add(buildTimePointDetail(point, day, isReward));
                    }
                }
                for (int point = 0; point < endPoint; point++) {
                    boolean isReward = isRewardPeriod(point);
                    if (isReward) rewardPoints++;
                    details.add(buildTimePointDetail(point, effectiveDays - 1, isReward));
                }
            }
        }

        double rewardRatio = (double) rewardPoints / totalPoints;
        double effectiveChargingKwh = chargingKwh * rewardRatio;
        double totalReward = effectiveChargingKwh * rewardPerKwh;

        String dominantType = getDominantTimeSlotTypeForMultiDay(startPoint, endPoint, effectiveDays);

        return ChargingRewardResponse.builder()
                .startPoint(startPoint)
                .endPoint(endPoint)
                .startTime(pointToTime(startPoint))
                .endTime(pointToTime(endPoint))
                .chargingKwh(chargingKwh)
                .rewardPerKwh(rewardPerKwh)
                .totalReward(Math.round(totalReward * 100.0) / 100.0)
                .timeSlotType(dominantType)
                .isRewardPeriod(rewardPoints > 0)
                .isCrossDay(isCrossDay)
                .days(effectiveDays)
                .totalPoints(totalPoints)
                .rewardPoints(rewardPoints)
                .timePointDetails(details)
                .build();
    }

    private ChargingRewardResponse.TimePointDetail buildTimePointDetail(int point, int day, boolean isReward) {
        return ChargingRewardResponse.TimePointDetail.builder()
                .point(point)
                .day(day)
                .timeRange(pointToTimeRange(point))
                .timeSlotType(getTimeSlotType(point).getName())
                .isRewardPeriod(isReward)
                .build();
    }

    private String getDominantTimeSlotTypeForMultiDay(int startPoint, int endPoint, int days) {
        int valleyCount = 0, peakCount = 0, normalCount = 0;

        if (days == 1) {
            if (endPoint <= startPoint) {
                for (int point = startPoint; point < TOTAL_POINTS; point++) {
                    TimeSlotType type = getTimeSlotType(point);
                    switch (type) {
                        case VALLEY: valleyCount++; break;
                        case PEAK: peakCount++; break;
                        case NORMAL: normalCount++; break;
                    }
                }
                for (int point = 0; point < endPoint; point++) {
                    TimeSlotType type = getTimeSlotType(point);
                    switch (type) {
                        case VALLEY: valleyCount++; break;
                        case PEAK: peakCount++; break;
                        case NORMAL: normalCount++; break;
                    }
                }
            } else {
                for (int point = startPoint; point < endPoint; point++) {
                    TimeSlotType type = getTimeSlotType(point);
                    switch (type) {
                        case VALLEY: valleyCount++; break;
                        case PEAK: peakCount++; break;
                        case NORMAL: normalCount++; break;
                    }
                }
            }
        } else {
            int[] counts = countTimeSlotTypesForFullDay();
            valleyCount = counts[0] * (days - 1);
            peakCount = counts[1] * (days - 1);
            normalCount = counts[2] * (days - 1);

            for (int point = startPoint; point < TOTAL_POINTS; point++) {
                TimeSlotType type = getTimeSlotType(point);
                switch (type) {
                    case VALLEY: valleyCount++; break;
                    case PEAK: peakCount++; break;
                    case NORMAL: normalCount++; break;
                }
            }
            for (int point = 0; point < endPoint; point++) {
                TimeSlotType type = getTimeSlotType(point);
                switch (type) {
                    case VALLEY: valleyCount++; break;
                    case PEAK: peakCount++; break;
                    case NORMAL: normalCount++; break;
                }
            }
        }

        if (valleyCount >= peakCount && valleyCount >= normalCount) {
            return TimeSlotType.VALLEY.getName();
        } else if (peakCount >= normalCount) {
            return TimeSlotType.PEAK.getName();
        } else {
            return TimeSlotType.NORMAL.getName();
        }
    }

    private int[] countTimeSlotTypesForFullDay() {
        int valleyCount = 0, peakCount = 0, normalCount = 0;
        for (int point = 0; point < TOTAL_POINTS; point++) {
            TimeSlotType type = getTimeSlotType(point);
            switch (type) {
                case VALLEY: valleyCount++; break;
                case PEAK: peakCount++; break;
                case NORMAL: normalCount++; break;
            }
        }
        return new int[]{valleyCount, peakCount, normalCount};
    }

    public String pointToTime(int point) {
        if (point == TOTAL_POINTS) {
            return "24:00";
        }
        validatePoint(point);
        int totalMinutes = point * 15;
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    public String pointToTimeRange(int point) {
        validatePoint(point);
        String startTime = pointToTime(point);
        String endTime = pointToTime((point + 1) % TOTAL_POINTS);
        return startTime + "-" + endTime;
    }

    public int timeToPoint(String time) {
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int totalMinutes = hours * 60 + minutes;
        return totalMinutes / 15;
    }

    private void validateStartPoint(int point) {
        if (point < 0 || point >= TOTAL_POINTS) {
            throw new IllegalArgumentException("开始时间点必须在0-95之间");
        }
    }

    private void validateEndPoint(int point) {
        if (point < 1 || point > TOTAL_POINTS) {
            throw new IllegalArgumentException("结束时间点必须在1-96之间");
        }
    }

    private void validatePoint(int point) {
        if (point < 0 || point >= TOTAL_POINTS) {
            throw new IllegalArgumentException("时间点必须在0-95之间");
        }
    }

    public List<TimeSlotInfo> getAllTimeSlots() {
        List<TimeSlotInfo> slots = new ArrayList<>();
        for (int i = 0; i < TOTAL_POINTS; i++) {
            slots.add(new TimeSlotInfo(i, pointToTimeRange(i), getTimeSlotType(i), isRewardPeriod(i)));
        }
        return slots;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class TimeSlotInfo {
        private int point;
        private String timeRange;
        private TimeSlotType type;
        private boolean isRewardPeriod;
    }
}
