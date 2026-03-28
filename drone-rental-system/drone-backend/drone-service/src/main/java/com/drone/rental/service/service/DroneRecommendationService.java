package com.drone.rental.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.drone.rental.dao.entity.DroneRecommendationRule;

import java.util.List;
import java.util.Map;

/**
 * 推荐服务接口
 */
public interface DroneRecommendationService extends IService<DroneRecommendationRule> {

    /**
     * 获取所有可用场景
     */
    List<Map<String, Object>> getAvailableScenarios();

    /**
     * 根据场景推荐无人机
     */
    List<Map<String, Object>> recommendByScenario(String scenario, Integer duration);

    /**
     * 根据预算推荐无人机
     */
    List<Map<String, Object>> recommendByBudget(Double minBudget, Double maxBudget);

    /**
     * 智能推荐（综合场景、预算、时长）
     */
    Map<String, Object> smartRecommend(Long userId, String scenario, Double budget, Integer duration);

    /**
     * 保存推荐历史
     */
    void saveRecommendationHistory(Long userId, String scenario, Double budget, Integer duration, List<Map<String, Object>> recommendations);

    /**
     * 获取用户推荐历史
     */
    List<Map<String, Object>> getUserRecommendationHistory(Long userId);
}
