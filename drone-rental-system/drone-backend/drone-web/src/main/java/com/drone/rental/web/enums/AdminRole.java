package com.drone.rental.web.enums;

import lombok.Getter;

/**
 * 管理员角色枚举
 */
@Getter
public enum AdminRole {

    SUPER("SUPER", "超级管理员"),
    ADMIN("ADMIN", "管理员");

    private final String code;
    private final String desc;

    AdminRole(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static AdminRole getByCode(String code) {
        for (AdminRole role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return ADMIN;
    }
}
