package com.drone.rental.service.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.drone.rental.dao.entity.UserRechargeRecord;

import java.math.BigDecimal;

/**
 * 用户充值记录服务接口
 */
public interface UserRechargeRecordService extends IService<UserRechargeRecord> {

    /**
     * 创建充值记录
     */
    UserRechargeRecord createRecharge(Long userId, String username, BigDecimal amount, String paymentMethod, String remark);

    /**
     * 充值成功回调
     */
    boolean rechargeSuccess(String rechargeNo);

    /**
     * 充值失败
     */
    boolean rechargeFail(String rechargeNo, String failReason);

    /**
     * 分页查询充值记录
     */
    IPage<UserRechargeRecord> selectPage(IPage<UserRechargeRecord> page, Long userId, String keyword);

    /**
     * 生成充值单号
     */
    String generateRechargeNo();
}
