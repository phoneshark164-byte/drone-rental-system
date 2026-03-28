package com.drone.rental.web.controller;

import com.drone.rental.common.constant.RedisKey;
import com.drone.rental.dao.entity.SysUser;
import com.drone.rental.service.service.SysUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 用户认证控制器
 */
@Controller
public class UserAuthController {

    @Autowired
    private SysUserService userService;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 用户登录
     */
    @PostMapping("/user/doLogin")
    public String login(@RequestParam String username,
                       @RequestParam String password,
                       @RequestParam(required = false) String captchaKey,
                       @RequestParam(required = false) String captchaCode,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        try {
            // 验证验证码
            if (!verifyCaptcha(captchaKey, captchaCode, session)) {
                redirectAttributes.addFlashAttribute("error", "验证码错误或已过期");
                return "redirect:/user/login";
            }

            SysUser user = userService.login(username, password);
            session.setAttribute("user", user);
            return "redirect:/user/index";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/login";
        }
    }

    /**
     * 验证验证码
     */
    private boolean verifyCaptcha(String captchaKey, String userInputCode, HttpSession session) {
        if (captchaKey == null || userInputCode == null || userInputCode.trim().isEmpty()) {
            return false;
        }

        String storedCode;
        if (stringRedisTemplate != null) {
            String redisKey = RedisKey.CAPTCHA_CODE + captchaKey;
            storedCode = stringRedisTemplate.opsForValue().get(redisKey);
            // 验证后删除验证码（一次性使用）
            if (storedCode != null) {
                stringRedisTemplate.delete(redisKey);
            }
        } else {
            // Redis不可用时使用session
            String sessionKey = (String) session.getAttribute("captchaKey");
            if (!captchaKey.equals(sessionKey)) {
                return false;
            }
            storedCode = (String) session.getAttribute("captchaCode");
            session.removeAttribute("captchaCode");
            session.removeAttribute("captchaKey");
        }

        return storedCode != null && storedCode.equals(userInputCode.toLowerCase().trim());
    }

    /**
     * 用户注册
     */
    @PostMapping("/user/doRegister")
    public String register(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String confirmPassword,
                          @RequestParam String phone,
                          @RequestParam String realName,
                          RedirectAttributes redirectAttributes) {
        try {
            if (!password.equals(confirmPassword)) {
                throw new RuntimeException("两次密码不一致");
            }
            userService.register(username, password, phone, realName);
            redirectAttributes.addFlashAttribute("success", "注册成功，请登录");
            return "redirect:/user/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/register";
        }
    }

    /**
     * 退出登录
     */
    @RequestMapping("/user/logout")
    public String logout(HttpSession session) {
        // 销毁session，彻底清除所有登录状态，防止会话重用
        session.invalidate();
        return "redirect:/user/login";
    }
}
