package com.drone.rental.common.constant;

/**
 * Redis键常量
 */
public class RedisKey {

    // 用户相关
    public static final String USER_INFO = "user:info:";
    public static final String USER_TOKEN = "user:token:";
    public static final String USER_LOGIN_LIMIT = "user:login:limit:";

    // 无人机相关
    public static final String VEHICLE_INFO = "drone:info:";
    public static final String VEHICLE_LIST = "drone:list";
    public static final String VEHICLE_AVAILABLE = "drone:available";

    // 订单相关
    public static final String ORDER_INFO = "order:info:";
    public static final String ORDER_USER_LIST = "order:user:";

    // 规则相关
    public static final String RULE_CACHE = "rule:cache:";

    // 验证码
    public static final String SMS_CODE = "sms:code:";
    public static final String EMAIL_CODE = "email:code:";

    // 限流
    public static final String RATE_LIMIT = "rate:limit:";

    // 封禁用户
    public static final String BLACKLIST_USER = "blacklist:user:";

    private RedisKey() {
    }
}
