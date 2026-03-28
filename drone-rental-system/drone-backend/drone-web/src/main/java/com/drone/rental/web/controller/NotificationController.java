package com.drone.rental.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drone.rental.dao.entity.SysNotification;
import com.drone.rental.dao.entity.SysUser;
import com.drone.rental.service.service.SysNotificationService;
import com.drone.rental.service.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知控制器
 */
@RestController
public class NotificationController {

    @Autowired
    private SysNotificationService notificationService;

    @Autowired
    private SysUserService userService;

    /**
     * 创建通知
     */
    @PostMapping("/admin/api/notification/create")
    public ApiResult<Void> createNotification(@RequestBody Map<String, Object> params) {
        SysNotification notification = new SysNotification();
        notification.setTitle((String) params.get("title"));
        notification.setContent((String) params.get("content"));
        notification.setType((String) params.getOrDefault("type", "info"));
        notification.setReceiverType((String) params.get("receiverType"));
        notification.setRelatedType((String) params.get("relatedType"));

        Object relatedId = params.get("relatedId");
        if (relatedId != null) {
            notification.setRelatedId(((Number) relatedId).longValue());
        }

        Object receiverId = params.get("receiverId");
        if (receiverId != null) {
            notification.setReceiverId(((Number) receiverId).longValue());
        }

        notificationService.save(notification);
        return ApiResult.success();
    }

    /**
     * 获取管理员通知列表
     */
    @GetMapping("/admin/api/notification/list")
    public ApiResult<List<SysNotification>> getAdminNotifications(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotification::getReceiverType, "admin")
                .or().eq(SysNotification::getReceiverType, "all")
                .orderByDesc(SysNotification::getCreateTime);

        List<SysNotification> list = notificationService.list(wrapper);
        return ApiResult.success(list);
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/admin/api/notification/unread-count")
    public ApiResult<Map<String, Object>> getUnreadCount() {
        long count = notificationService.lambdaQuery()
                .eq(SysNotification::getReceiverType, "admin")
                .or().eq(SysNotification::getReceiverType, "all")
                .eq(SysNotification::getIsRead, 0)
                .count();

        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return ApiResult.success(result);
    }

    /**
     * 标记通知为已读
     */
    @PostMapping("/admin/api/notification/{id}/read")
    public ApiResult<Void> markAsRead(@PathVariable Long id) {
        SysNotification notification = notificationService.getById(id);
        if (notification != null) {
            notification.setIsRead(1);
            notification.setReadTime(LocalDateTime.now());
            notificationService.updateById(notification);
        }
        return ApiResult.success();
    }

    /**
     * 标记所有通知为已读
     */
    @PostMapping("/admin/api/notification/read-all")
    public ApiResult<Void> markAllAsRead() {
        List<SysNotification> unreadList = notificationService.lambdaQuery()
                .eq(SysNotification::getReceiverType, "admin")
                .or().eq(SysNotification::getReceiverType, "all")
                .eq(SysNotification::getIsRead, 0)
                .list();

        LocalDateTime now = LocalDateTime.now();
        for (SysNotification notification : unreadList) {
            notification.setIsRead(1);
            notification.setReadTime(now);
        }

        if (!unreadList.isEmpty()) {
            notificationService.updateBatchById(unreadList);
        }

        return ApiResult.success();
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/admin/api/notification/{id}")
    public ApiResult<Void> deleteNotification(@PathVariable Long id) {
        notificationService.removeById(id);
        return ApiResult.success();
    }

    // ========== 用户通知API ==========

    /**
     * 获取用户通知列表
     */
    @GetMapping("/user/api/notification/list")
    public ApiResult<List<SysNotification>> getUserNotifications(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.fail("请先登录");
        }

        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(SysNotification::getReceiverType, "user")
                .or().eq(SysNotification::getReceiverType, "all"))
                .or(w -> w.eq(SysNotification::getReceiverType, "specific")
                        .eq(SysNotification::getReceiverId, user.getId()))
                .orderByDesc(SysNotification::getCreateTime);

        List<SysNotification> list = notificationService.list(wrapper);
        return ApiResult.success(list);
    }

    /**
     * 获取用户未读通知数量
     */
    @GetMapping("/user/api/notification/unread-count")
    public ApiResult<Map<String, Object>> getUserUnreadCount(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.fail("请先登录");
        }

        long count = notificationService.lambdaQuery()
                .and(w -> w.eq(SysNotification::getReceiverType, "user")
                        .or().eq(SysNotification::getReceiverType, "all"))
                .or(w -> w.eq(SysNotification::getReceiverType, "specific")
                        .eq(SysNotification::getReceiverId, user.getId()))
                .eq(SysNotification::getIsRead, 0)
                .count();

        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return ApiResult.success(result);
    }

    /**
     * 标记用户通知为已读
     */
    @PostMapping("/user/api/notification/{id}/read")
    public ApiResult<Void> markUserAsRead(@PathVariable Long id, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.fail("请先登录");
        }

        SysNotification notification = notificationService.getById(id);
        if (notification != null) {
            notification.setIsRead(1);
            notification.setReadTime(LocalDateTime.now());
            notificationService.updateById(notification);
        }
        return ApiResult.success();
    }

    /**
     * 标记所有用户通知为已读
     */
    @PostMapping("/user/api/notification/read-all")
    public ApiResult<Void> markUserAllAsRead(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.fail("请先登录");
        }

        List<SysNotification> unreadList = notificationService.lambdaQuery()
                .and(w -> w.eq(SysNotification::getReceiverType, "user")
                        .or().eq(SysNotification::getReceiverType, "all"))
                .or(w -> w.eq(SysNotification::getReceiverType, "specific")
                        .eq(SysNotification::getReceiverId, user.getId()))
                .eq(SysNotification::getIsRead, 0)
                .list();

        LocalDateTime now = LocalDateTime.now();
        for (SysNotification notification : unreadList) {
            notification.setIsRead(1);
            notification.setReadTime(now);
        }

        if (!unreadList.isEmpty()) {
            notificationService.updateBatchById(unreadList);
        }

        return ApiResult.success();
    }

    /**
     * 删除用户通知
     */
    @DeleteMapping("/user/api/notification/{id}")
    public ApiResult<Void> deleteUserNotification(@PathVariable Long id, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.fail("请先登录");
        }

        notificationService.removeById(id);
        return ApiResult.success();
    }
}
