package com.drone.rental.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drone.rental.dao.entity.UserRecommendationHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户推荐历史Mapper
 */
@Mapper
public interface UserRecommendationHistoryMapper extends BaseMapper<UserRecommendationHistory> {
}
