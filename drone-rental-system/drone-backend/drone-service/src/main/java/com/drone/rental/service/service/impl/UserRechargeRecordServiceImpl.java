package com.drone.rental.service.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.common.exception.BusinessException;
import com.drone.rental.dao.entity.SysUser;
import com.drone.rental.dao.entity.UserRechargeRecord;
import com.drone.rental.dao.mapper.UserRechargeRecordMapper;
import com.drone.rental.service.service.SysUserService;
import com.drone.rental.service.service.UserRechargeRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用户充值记录服务实现
 */
@Service
public class UserRechargeRecordServiceImpl extends ServiceImpl<UserRechargeRecordMapper, UserRechargeRecord> implements UserRechargeRecordService {

    @Autowired
    private SysUserService userService;

    @Override
    public String generateRechargeNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        long random = (long) (Math.random() * 10000);
        return "RC" + timestamp + String.format("%04d", random);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserRechargeRecord createRecharge(Long userId, String username, BigDecimal amount, String paymentMethod, String remark) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("充值金额必须大于0");
        }

        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        UserRechargeRecord record = new UserRechargeRecord();
        record.setRechargeNo(generateRechargeNo());
        record.setUserId(userId);
        record.setUsername(username);
        record.setAmount(amount);
        record.setPaymentMethod(paymentMethod);
        record.setStatus(0);
        record.setRemark(remark);
        save(record);

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rechargeSuccess(String rechargeNo) {
        UserRechargeRecord record = getOne(new LambdaQueryWrapper<UserRechargeRecord>()
                .eq(UserRechargeRecord::getRechargeNo, rechargeNo));
        if (record == null) {
            throw new BusinessException("充值记录不存在");
        }
        if (record.getStatus() == 1) {
            throw new BusinessException("充值已完成，请勿重复操作");
        }

        // 更新充值记录状态
        record.setStatus(1);
        updateById(record);

        // 增加用户余额
        SysUser user = userService.getById(record.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        user.setBalance(currentBalance.add(record.getAmount()));
        userService.updateById(user);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rechargeFail(String rechargeNo, String failReason) {
        UserRechargeRecord record = getOne(new LambdaQueryWrapper<UserRechargeRecord>()
                .eq(UserRechargeRecord::getRechargeNo, rechargeNo));
        if (record == null) {
            throw new BusinessException("充值记录不存在");
        }
        if (record.getStatus() != 0) {
            throw new BusinessException("充值记录状态异常");
        }

        record.setStatus(2);
        record.setRemark(record.getRemark() + " 失败原因:" + failReason);
        return updateById(record);
    }

    @Override
    public IPage<UserRechargeRecord> selectPage(IPage<UserRechargeRecord> page, Long userId, String keyword) {
        LambdaQueryWrapper<UserRechargeRecord> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(UserRechargeRecord::getUserId, userId);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(UserRechargeRecord::getUsername, keyword)
                    .or().like(UserRechargeRecord::getRechargeNo, keyword);
        }
        wrapper.orderByDesc(UserRechargeRecord::getCreateTime);
        return page(page, wrapper);
    }
}
