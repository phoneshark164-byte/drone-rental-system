package com.drone.rental.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.common.exception.BusinessException;
import com.drone.rental.common.utils.PasswordUtil;
import com.drone.rental.dao.entity.SysAdmin;
import com.drone.rental.dao.mapper.SysAdminMapper;
import com.drone.rental.service.service.SysAdminService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 管理员服务实现
 */
@Service
public class SysAdminServiceImpl extends ServiceImpl<SysAdminMapper, SysAdmin> implements SysAdminService {

    @Override
    public SysAdmin login(String username, String password) {
        SysAdmin admin = getByUsername(username);
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        if (admin.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        if (!PasswordUtil.matches(password, admin.getPassword())) {
            throw new BusinessException("密码错误");
        }
        // 更新最后登录时间
        admin.setLastLoginTime(LocalDateTime.now());
        updateById(admin);
        return admin;
    }

    @Override
    public SysAdmin getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<SysAdmin>().eq(SysAdmin::getUsername, username));
    }
}
