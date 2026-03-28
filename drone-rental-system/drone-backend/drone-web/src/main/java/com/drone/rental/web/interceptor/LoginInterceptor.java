package com.drone.rental.web.interceptor;

import com.drone.rental.web.enums.AdminRole;
import com.drone.rental.dao.entity.SysAdmin;
import com.drone.rental.dao.entity.SysOperator;
import com.drone.rental.dao.entity.SysUser;
import com.drone.rental.service.service.SysAdminService;
import com.drone.rental.service.service.SysOperatorService;
import com.drone.rental.service.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private SysUserService userService;

    @Autowired
    private SysOperatorService operatorService;

    @Autowired
    private SysAdminService adminService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        HttpSession session = request.getSession();

        if (uri.startsWith("/user/")) {
            SysUser user = (SysUser) session.getAttribute("user");
            if (user == null) {
                response.sendRedirect("/user/login");
                return false;
            }
        } else if (uri.startsWith("/operator/")) {
            SysOperator operator = (SysOperator) session.getAttribute("operator");
            if (operator == null) {
                response.sendRedirect("/operator/login");
                return false;
            }
        } else if (uri.startsWith("/admin/")) {
            SysAdmin admin = (SysAdmin) session.getAttribute("admin");
            if (admin == null) {
                response.sendRedirect("/admin/login");
                return false;
            }
        }

        return true;
    }
}
