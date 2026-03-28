package com.drone.rental.service.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.common.exception.BusinessException;
import com.drone.rental.dao.entity.DroneOrder;
import com.drone.rental.dao.entity.SysUser;
import com.drone.rental.dao.entity.UserPaymentRecord;
import com.drone.rental.dao.mapper.UserPaymentRecordMapper;
import com.drone.rental.service.service.DroneOrderService;
import com.drone.rental.service.service.SysUserService;
import com.drone.rental.service.service.UserPaymentRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用户支付记录服务实现
 */
@Service
public class UserPaymentRecordServiceImpl extends ServiceImpl<UserPaymentRecordMapper, UserPaymentRecord> implements UserPaymentRecordService {

    @Autowired
    private SysUserService userService;

    @Autowired
    private DroneOrderService orderService;

    @Override
    public String generatePaymentNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        long random = (long) (Math.random() * 10000);
        return "PAY" + timestamp + String.format("%04d", random);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserPaymentRecord createPayment(Long userId, String username, Long orderId, String orderNo, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("支付金额必须大于0");
        }

        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        DroneOrder order = orderService.getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        UserPaymentRecord record = new UserPaymentRecord();
        record.setPaymentNo(generatePaymentNo());
        record.setUserId(userId);
        record.setUsername(username);
        record.setOrderId(orderId);
        record.setOrderNo(orderNo);
        record.setAmount(amount);
        record.setBalanceBefore(user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO);
        record.setStatus(0);
        save(record);

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean payOrderByBalance(String paymentNo) {
        UserPaymentRecord record = getOne(new LambdaQueryWrapper<UserPaymentRecord>()
                .eq(UserPaymentRecord::getPaymentNo, paymentNo));
        if (record == null) {
            throw new BusinessException("支付记录不存在");
        }
        if (record.getStatus() != 0) {
            throw new BusinessException("支付记录状态异常");
        }

        SysUser user = userService.getById(record.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        BigDecimal balance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        if (balance.compareTo(record.getAmount()) < 0) {
            throw new BusinessException("余额不足，当前余额: " + balance + "元");
        }

        // 扣除余额
        user.setBalance(balance.subtract(record.getAmount()));
        userService.updateById(user);

        // 更新支付记录
        record.setStatus(1);
        record.setBalanceAfter(user.getBalance());
        updateById(record);

        // 更新订单状态
        DroneOrder order = orderService.getById(record.getOrderId());
        if (order != null) {
            order.setStatus(1); // 已支付
            order.setPayTime(LocalDateTime.now());
            orderService.updateById(order);
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean paymentSuccess(String paymentNo) {
        UserPaymentRecord record = getOne(new LambdaQueryWrapper<UserPaymentRecord>()
                .eq(UserPaymentRecord::getPaymentNo, paymentNo));
        if (record == null) {
            throw new BusinessException("支付记录不存在");
        }
        record.setStatus(1);
        return updateById(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean paymentFail(String paymentNo, String failReason) {
        UserPaymentRecord record = getOne(new LambdaQueryWrapper<UserPaymentRecord>()
                .eq(UserPaymentRecord::getPaymentNo, paymentNo));
        if (record == null) {
            throw new BusinessException("支付记录不存在");
        }
        if (record.getStatus() != 0) {
            throw new BusinessException("支付记录状态异常");
        }

        record.setStatus(2);
        record.setFailReason(failReason);
        return updateById(record);
    }

    @Override
    public IPage<UserPaymentRecord> selectPage(IPage<UserPaymentRecord> page, Long userId, String keyword) {
        LambdaQueryWrapper<UserPaymentRecord> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(UserPaymentRecord::getUserId, userId);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(UserPaymentRecord::getUsername, keyword)
                    .or().like(UserPaymentRecord::getOrderNo, keyword)
                    .or().like(UserPaymentRecord::getPaymentNo, keyword);
        }
        wrapper.orderByDesc(UserPaymentRecord::getCreateTime);
        return page(page, wrapper);
    }
}
