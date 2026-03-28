package com.drone.rental.service.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.common.enums.OrderStatus;
import com.drone.rental.common.enums.VehicleStatus;
import com.drone.rental.common.exception.BusinessException;
import com.drone.rental.dao.entity.DroneOrder;
import com.drone.rental.dao.entity.DroneVehicle;
import com.drone.rental.dao.entity.SysUser;
import com.drone.rental.dao.entity.SysNotification;
import com.drone.rental.dao.mapper.DroneOrderMapper;
import com.drone.rental.dao.mapper.DroneVehicleMapper;
import com.drone.rental.service.service.DroneOrderService;
import com.drone.rental.service.service.DroneVehicleService;
import com.drone.rental.service.service.SysUserService;
import com.drone.rental.service.service.SysNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单服务实现
 */
@Service
public class DroneOrderServiceImpl extends ServiceImpl<DroneOrderMapper, DroneOrder> implements DroneOrderService {

    @Autowired
    private DroneVehicleService vehicleService;

    @Autowired
    private SysUserService userService;

    @Autowired
    private SysNotificationService notificationService;

    @Override
    @Transactional
    public DroneOrder createOrder(Long userId, Long vehicleId, Integer plannedDuration,
                                   Double latitude, Double longitude, String location) {
        DroneVehicle vehicle = vehicleService.getById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException("无人机不存在");
        }
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE.getCode()) {
            throw new BusinessException("无人机不可用");
        }

        // 计算订单金额（默认每分钟0.5元，即每小时30元）
        BigDecimal hourlyRate = new BigDecimal("30.00");
        BigDecimal amount = hourlyRate.multiply(new BigDecimal(plannedDuration)).divide(new BigDecimal("60"), 2, BigDecimal.ROUND_HALF_UP);

        DroneOrder order = new DroneOrder();
        order.setOrderNo(IdUtil.simpleUUID());
        order.setUserId(userId);
        order.setVehicleId(vehicleId);
        order.setVehicleNo(vehicle.getVehicleNo());
        order.setPlannedDuration(plannedDuration);
        order.setAmount(amount);
        order.setStartLatitude(BigDecimal.valueOf(latitude));
        order.setStartLongitude(BigDecimal.valueOf(longitude));
        order.setStartLocation(location);
        order.setStatus(OrderStatus.UNPAID.getCode());
        save(order);

        // 更新无人机状态为使用中
        vehicleService.updateStatus(vehicleId, VehicleStatus.IN_USE.getCode());

        return order;
    }

    @Override
    @Transactional
    public void payOrder(Long orderId) {
        DroneOrder order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setStatus(OrderStatus.PAID.getCode());
        order.setPayTime(LocalDateTime.now());
        updateById(order);
    }

    @Override
    @Transactional
    public void startUse(Long orderId) {
        DroneOrder order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setStatus(OrderStatus.IN_PROGRESS.getCode());
        order.setStartTime(LocalDateTime.now());
        updateById(order);
    }

    @Override
    @Transactional
    public void endUse(Long orderId, Double latitude, Double longitude, String location) {
        DroneOrder order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setEndLatitude(BigDecimal.valueOf(latitude));
        order.setEndLongitude(BigDecimal.valueOf(longitude));
        order.setEndLocation(location);
        order.setEndTime(LocalDateTime.now());
        order.setStatus(OrderStatus.COMPLETED.getCode());
        updateById(order);

        // 释放无人机
        DroneOrder dbOrder = getById(orderId);
        vehicleService.updateStatus(dbOrder.getVehicleId(), VehicleStatus.AVAILABLE.getCode());

        // 自动退还押金到用户账户
        SysUser user = userService.getById(order.getUserId());
        if (user != null && user.getDeposit() != null && user.getDeposit().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal depositAmount = user.getDeposit();

            // 退还押金到余额
            user.setBalance(user.getBalance().add(depositAmount));
            // 清零押金
            user.setDeposit(BigDecimal.ZERO);
            userService.updateById(user);

            // 发送通知给用户
            SysNotification notification = new SysNotification();
            notification.setTitle("押金已退还");
            notification.setContent(String.format("您的订单已完成，押金 ¥%s 已退还到账户余额", depositAmount.toString()));
            notification.setType("success");
            notification.setReceiverType("specific");
            notification.setReceiverId(user.getId());
            notification.setRelatedType("order");
            notification.setRelatedId(order.getId());
            notificationService.save(notification);
        }
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        DroneOrder order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason);
        updateById(order);

        // 释放无人机
        vehicleService.updateStatus(order.getVehicleId(), VehicleStatus.AVAILABLE.getCode());
    }

    @Override
    public DroneOrder getByOrderNo(String orderNo) {
        return getOne(new LambdaQueryWrapper<DroneOrder>().eq(DroneOrder::getOrderNo, orderNo));
    }

    @Override
    public IPage<DroneOrder> selectUserPage(IPage<DroneOrder> page, Long userId, Integer status) {
        LambdaQueryWrapper<DroneOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DroneOrder::getUserId, userId);
        if (status != null) {
            wrapper.eq(DroneOrder::getStatus, status);
        }
        wrapper.orderByDesc(DroneOrder::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public IPage<DroneOrder> selectPage(IPage<DroneOrder> page, String keyword, Integer status) {
        LambdaQueryWrapper<DroneOrder> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DroneOrder::getOrderNo, keyword)
                    .or().like(DroneOrder::getVehicleNo, keyword);
        }
        if (status != null) {
            wrapper.eq(DroneOrder::getStatus, status);
        }
        wrapper.orderByDesc(DroneOrder::getCreateTime);
        return page(page, wrapper);
    }
}
