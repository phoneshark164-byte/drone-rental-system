package com.drone.rental.common.constant;

/**
 * 系统常量
 */
public class SystemConstant {

    // 默认密码
    public static final String DEFAULT_PASSWORD = "123456";

    // token过期时间 秒
    public static final long TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60;

    // 验证码过期时间 秒
    public static final long CODE_EXPIRE_TIME = 5 * 60;

    // 分页默认值
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 10;

    // 文件上传路径
    public static final String UPLOAD_PATH = "/uploads/";

    // 头像默认路径
    public static final String DEFAULT_AVATAR = "/images/default-avatar.png";

    // 管理员session key
    public static final String ADMIN_SESSION_KEY = "admin_session";

    // 运营方session key
    public static final String OPERATOR_SESSION_KEY = "operator_session";

    // 用户session key
    public static final String USER_SESSION_KEY = "user_session";

    private SystemConstant() {
    }
}
