package com.drone.rental.web.utils;

import cn.hutool.crypto.digest.BCrypt;

/**
 * 密码工具类
 */
public class PasswordUtil {

    /**
     * 加密密码
     */
    public static String encode(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * 匹配密码
     */
    public static boolean matches(String password, String encodedPassword) {
        return BCrypt.checkpw(password, encodedPassword);
    }

    private PasswordUtil() {
    }
}
