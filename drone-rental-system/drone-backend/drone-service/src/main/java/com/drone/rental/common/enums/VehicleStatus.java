package com.drone.rental.common.enums;

import lombok.Getter;

/**
 * 无人机状态枚举
 */
@Getter
public enum VehicleStatus {

    FAULT(0, "故障"),
    AVAILABLE(1, "空闲"),
    IN_USE(2, "使用中"),
    CHARGING(3, "充电中"),
    MAINTENANCE(4, "维护中");

    private final Integer code;
    private final String desc;

    VehicleStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static VehicleStatus getByCode(Integer code) {
        for (VehicleStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return FAULT;
    }
}
