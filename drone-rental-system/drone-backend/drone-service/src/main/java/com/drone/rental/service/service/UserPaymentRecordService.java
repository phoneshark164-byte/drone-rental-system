package com.drone.rental.service.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.drone.rental.dao.entity.UserPaymentRecord;

import java.math.BigDecimal;

/**
 * 用户支付记录服务接口
 */
public interface UserPaymentRecordService extends IService<UserPaymentRecord> {

    /**
     * 创建支付记录
     */
    UserPaymentRecord createPayment(Long userId, String username, Long orderId, String orderNo, BigDecimal amount);

    /**
     * 余额支付订单
     */
    boolean payOrderByBalance(String paymentNo);

    /**
     * 支付成功回调
     */
    boolean paymentSuccess(String paymentNo);

    /**
     * 支付失败
     */
    boolean paymentFail(String paymentNo, String failReason);

    /**
     * 分页查询支付记录
     */
    IPage<UserPaymentRecord> selectPage(IPage<UserPaymentRecord> page, Long userId, String keyword);

    /**
     * 生成支付单号
     */
    String generatePaymentNo();
}
