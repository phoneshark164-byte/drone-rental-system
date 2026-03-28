package com.drone.rental.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.dao.entity.DroneBanner;
import com.drone.rental.dao.mapper.DroneBannerMapper;
import com.drone.rental.service.service.DroneBannerService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 轮播图服务实现
 */
@Service
public class DroneBannerServiceImpl extends ServiceImpl<DroneBannerMapper, DroneBanner> implements DroneBannerService {

    @Override
    public IPage<DroneBanner> selectPage(IPage<DroneBanner> page, String keyword, Integer status) {
        LambdaQueryWrapper<DroneBanner> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(DroneBanner::getTitle, keyword)
                    .or().like(DroneBanner::getSubtitle, keyword);
        }
        if (status != null) {
            wrapper.eq(DroneBanner::getStatus, status);
        }
        wrapper.orderByAsc(DroneBanner::getSortOrder)
                .orderByDesc(DroneBanner::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public List<DroneBanner> getActiveBanners() {
        return list(new LambdaQueryWrapper<DroneBanner>()
                .eq(DroneBanner::getStatus, 1)
                .orderByAsc(DroneBanner::getSortOrder)
                .orderByDesc(DroneBanner::getCreateTime));
    }
}
