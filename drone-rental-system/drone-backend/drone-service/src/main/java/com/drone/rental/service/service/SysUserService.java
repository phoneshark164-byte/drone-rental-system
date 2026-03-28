package com.drone.rental.service.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.drone.rental.dao.entity.SysUser;

/**
 * 用户服务接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 用户登录
     */
    SysUser login(String username, String password);

    /**
     * 用户注册
     */
    void register(String username, String password, String phone, String realName);

    /**
     * 根据用户名查询
     */
    SysUser getByUsername(String username);

    /**
     * 分页查询用户
     */
    IPage<SysUser> selectPage(IPage<SysUser> page, String keyword);

    /**
     * 更新用户状态
     */
    void updateStatus(Long userId, Integer status);
}
