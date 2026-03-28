package com.drone.rental.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.drone.rental.dao.entity.SysOperator;

/**
 * 运营方服务接口
 */
public interface SysOperatorService extends IService<SysOperator> {

    /**
     * 运营方登录
     */
    SysOperator login(String username, String password);

    /**
     * 根据用户名查询
     */
    SysOperator getByUsername(String username);
}
