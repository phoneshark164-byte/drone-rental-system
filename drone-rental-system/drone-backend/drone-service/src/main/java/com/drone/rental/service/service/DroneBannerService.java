package com.drone.rental.service.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.drone.rental.dao.entity.DroneBanner;

/**
 * 轮播图服务接口
 */
public interface DroneBannerService extends IService<DroneBanner> {

    /**
     * 分页查询轮播图
     */
    IPage<DroneBanner> selectPage(IPage<DroneBanner> page, String keyword, Integer status);

    /**
     * 获取启用的轮播图列表（按排序号排序）
     */
    java.util.List<DroneBanner> getActiveBanners();
}
