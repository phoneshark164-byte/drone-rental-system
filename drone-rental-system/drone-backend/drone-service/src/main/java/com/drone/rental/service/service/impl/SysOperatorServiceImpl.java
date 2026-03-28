package com.drone.rental.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.common.exception.BusinessException;
import com.drone.rental.common.utils.PasswordUtil;
import com.drone.rental.dao.entity.SysOperator;
import com.drone.rental.dao.mapper.SysOperatorMapper;
import com.drone.rental.service.service.SysOperatorService;
import org.springframework.stereotype.Service;

/**
 * 运营方服务实现
 */
@Service
public class SysOperatorServiceImpl extends ServiceImpl<SysOperatorMapper, SysOperator> implements SysOperatorService {

    @Override
    public SysOperator login(String username, String password) {
        SysOperator operator = getByUsername(username);
        if (operator == null) {
            throw new BusinessException("运营方不存在");
        }
        if (operator.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        if (!PasswordUtil.matches(password, operator.getPassword())) {
            throw new BusinessException("密码错误");
        }
        return operator;
    }

    @Override
    public SysOperator getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<SysOperator>().eq(SysOperator::getUsername, username));
    }
}
