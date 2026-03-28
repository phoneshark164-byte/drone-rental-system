package com.drone.rental.service.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.drone.rental.dao.entity.DroneOrder;

/**
 * 订单服务接口
 */
public interface DroneOrderService extends IService<DroneOrder> {

    /**
     * 创建订单
     */
    DroneOrder createOrder(Long userId, Long vehicleId, Integer plannedDuration,
                       Double latitude, Double longitude, String location);

    /**
     * 支付订单
     */
    void payOrder(Long orderId);

    /**
     * 开始使用
     */
    void startUse(Long orderId);

    /**
     * 结束使用
     */
    void endUse(Long orderId, Double latitude, Double longitude, String location);

    /**
     * 取消订单
     */
    void cancelOrder(Long orderId, String reason);

    /**
     * 根据订单编号查询
     */
    DroneOrder getByOrderNo(String orderNo);

    /**
     * 分页查询用户订单
     */
    IPage<DroneOrder> selectUserPage(IPage<DroneOrder> page, Long userId, Integer status);

    /**
     * 分页查询所有订单
     */
    IPage<DroneOrder> selectPage(IPage<DroneOrder> page, String keyword, Integer status);
}
