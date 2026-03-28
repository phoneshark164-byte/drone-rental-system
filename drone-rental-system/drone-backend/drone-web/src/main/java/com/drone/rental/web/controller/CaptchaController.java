package com.drone.rental.web.controller;

import com.drone.rental.common.constant.RedisKey;
import com.drone.rental.common.result.ApiResult;
import com.drone.rental.common.utils.CaptchaUtil;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 验证码控制器
 */
@RestController
@RequestMapping("/api")
public class CaptchaController {

    private static final Logger log = LoggerFactory.getLogger(CaptchaController.class);

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成验证码
     */
    @GetMapping("/captcha")
    public ApiResult<Map<String, String>> generateCaptcha(HttpSession session) {
        // 生成验证码
        CaptchaUtil.Captcha captcha = CaptchaUtil.generateCaptcha();

        // 生成验证码唯一标识
        String captchaKey = UUID.randomUUID().toString();

        // 存储到Redis，5分钟过期
        if (stringRedisTemplate != null) {
            String redisKey = RedisKey.CAPTCHA_CODE + captchaKey;
            stringRedisTemplate.opsForValue().set(redisKey, captcha.getCode().toLowerCase(), 5, TimeUnit.MINUTES);
            log.info("验证码已存储到Redis，key: {}", redisKey);
        } else {
            // 如果Redis不可用，使用session存储
            session.setAttribute("captchaCode", captcha.getCode().toLowerCase());
            session.setAttribute("captchaKey", captchaKey);
            log.warn("Redis不可用，验证码存储到Session");
        }

        // 返回验证码图片和key
        Map<String, String> result = new HashMap<>();
        result.put("key", captchaKey);
        result.put("image", captcha.getImageBase64());

        return ApiResult.success(result);
    }

    /**
     * 验证验证码
     */
    public static boolean verifyCaptcha(String captchaKey, String userInputCode,
                                       StringRedisTemplate stringRedisTemplate,
                                       HttpSession session) {
        if (captchaKey == null || userInputCode == null) {
            return false;
        }

        String storedCode;
        if (stringRedisTemplate != null) {
            String redisKey = RedisKey.CAPTCHA_CODE + captchaKey;
            storedCode = stringRedisTemplate.opsForValue().get(redisKey);
            log.info("从Redis验证验证码，key: {}, 结果: {}", redisKey, storedCode != null ? "成功" : "失败");
            // 验证后删除验证码
            if (storedCode != null) {
                stringRedisTemplate.delete(redisKey);
            }
        } else {
            // 从session获取
            String sessionKey = (String) session.getAttribute("captchaKey");
            if (!captchaKey.equals(sessionKey)) {
                log.warn("Session验证码key不匹配");
                return false;
            }
            storedCode = (String) session.getAttribute("captchaCode");
            session.removeAttribute("captchaCode");
            session.removeAttribute("captchaKey");
            log.info("从Session验证验证码，结果: {}", storedCode != null ? "成功" : "失败");
        }

        return storedCode != null && storedCode.equals(userInputCode.toLowerCase());
    }
}
