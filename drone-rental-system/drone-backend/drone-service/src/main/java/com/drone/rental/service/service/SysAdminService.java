package com.drone.rental.service.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.drone.rental.dao.entity.SysAdmin;

/**
 * 管理员服务接口
 */
public interface SysAdminService extends IService<SysAdmin> {

    /**
     * 管理员登录
     */
    SysAdmin login(String username, String password);

    /**
     * 根据用户名查询
     */
    SysAdmin getByUsername(String username);
}
