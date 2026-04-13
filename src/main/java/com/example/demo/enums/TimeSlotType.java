package com.example.demo.enums;

public enum TimeSlotType {
    VALLEY("谷时", "鼓励充电，有奖励"),
    PEAK("峰时", "不鼓励充电，无奖励"),
    NORMAL("平时", "正常充电，无奖励");

    private final String name;
    private final String description;

    TimeSlotType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
