package com.drone.rental.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.dao.entity.UserRecommendationHistory;
import com.drone.rental.dao.mapper.UserRecommendationHistoryMapper;
import com.drone.rental.service.service.UserRecommendationHistoryService;
import org.springframework.stereotype.Service;

/**
 * 用户推荐历史服务实现
 */
@Service
public class UserRecommendationHistoryServiceImpl extends ServiceImpl<UserRecommendationHistoryMapper, UserRecommendationHistory>
        implements UserRecommendationHistoryService {
}
