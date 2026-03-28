package com.drone.rental.common.enums;

import lombok.Getter;

/**
 * 报修状态枚举
 */
@Getter
public enum RepairStatus {

    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    COMPLETED(2, "已完成"),
    CLOSED(3, "已关闭");

    private final Integer code;
    private final String desc;

    RepairStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static RepairStatus getByCode(Integer code) {
        for (RepairStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return PENDING;
    }
}
