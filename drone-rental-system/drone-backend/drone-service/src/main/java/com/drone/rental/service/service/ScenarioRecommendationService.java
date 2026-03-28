package com.drone.rental.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.drone.rental.dao.entity.ScenarioRecommendation;

/**
 * 场景推荐配置服务接口
 */
public interface ScenarioRecommendationService extends IService<ScenarioRecommendation> {

    /**
     * 根据场景代码获取配置
     */
    ScenarioRecommendation getByScenarioCode(String scenarioCode);
}
