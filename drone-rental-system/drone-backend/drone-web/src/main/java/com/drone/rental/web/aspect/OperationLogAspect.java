package com.drone.rental.web.aspect;

import com.drone.rental.dao.entity.DroneOperationLog;
import com.drone.rental.service.service.DroneOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志切面
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(OperationLogAspect.class);

    @Autowired
    private DroneOperationLogService operationLogService;

    /**
     * 定义切点：拦截所有控制器方法
     */
    @Pointcut("execution(* com.drone.rental.web.controller..*.*(..))")
    public void controllerPointcut() {
    }

    /**
     * 环绕通知：记录操作日志
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 只记录API接口请求（/api/路径）
        if (!uri.contains("/api/")) {
            return joinPoint.proceed();
        }

        DroneOperationLog log = new DroneOperationLog();
        log.setOperation(getOperationName(uri, method));
        log.setMethod(method + " " + uri);
        log.setParams(getParams(joinPoint, request));
        log.setIp(getClientIp(request));
        log.setLocation("-");
        log.setCreateTime(LocalDateTime.now());

        // 判断操作者类型
        String operatorType = "UNKNOWN";
        if (uri.startsWith("/user/api/")) {
            operatorType = "USER";
        } else if (uri.startsWith("/operator/api/")) {
            operatorType = "OPERATOR";
        } else if (uri.startsWith("/admin/api/")) {
            operatorType = "ADMIN";
        }
        log.setOperatorType(operatorType);

        Object result = null;
        Integer status = 1;
        String errorMsg = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            status = 0;
            errorMsg = e.getMessage();
            logger.error("操作执行失败: {}", e.getMessage(), e);
            throw e;
        } finally {
            long executeTime = System.currentTimeMillis() - startTime;
            log.setExecuteTime(executeTime);
            log.setStatus(status);
            log.setErrorMsg(errorMsg);

            // 异步保存日志
            try {
                operationLogService.save(log);
            } catch (Exception e) {
                logger.error("保存操作日志失败: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 获取操作名称
     */
    private String getOperationName(String uri, String method) {
        if (uri.contains("/login")) return "用户登录";
        if (uri.contains("/logout")) return "用户登出";
        if (uri.contains("/register")) return "用户注册";
        if (uri.contains("/vehicle")) {
            if (method.equals("POST")) return "添加无人机";
            if (method.equals("PUT")) return "修改无人机";
            if (method.equals("DELETE")) return "删除无人机";
            return "无人机操作";
        }
        if (uri.contains("/order")) {
            if (method.equals("POST")) return "创建订单";
            if (uri.contains("/pay")) return "支付订单";
            if (uri.contains("/start")) return "开始使用";
            if (uri.contains("/end")) return "结束使用";
            if (uri.contains("/cancel")) return "取消订单";
            return "订单操作";
        }
        if (uri.contains("/user")) return "用户管理";
        if (uri.contains("/banner")) return "轮播图管理";
        return "其他操作";
    }

    /**
     * 获取请求参数
     */
    private String getParams(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return "";
            }
            StringBuilder params = new StringBuilder();
            for (Object arg : args) {
                if (arg != null && !(arg instanceof HttpServletRequest) &&
                    !(arg instanceof jakarta.servlet.http.HttpServletResponse)) {
                    String paramStr = arg.toString();
                    if (paramStr.length() > 200) {
                        paramStr = paramStr.substring(0, 200) + "...";
                    }
                    params.append(paramStr).append("; ");
                }
            }
            return params.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP的情况，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
