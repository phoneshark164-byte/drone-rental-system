package com.drone.rental.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.dao.entity.ScenarioRecommendation;
import com.drone.rental.dao.mapper.ScenarioRecommendationMapper;
import com.drone.rental.service.service.ScenarioRecommendationService;
import org.springframework.stereotype.Service;

/**
 * 场景推荐配置服务实现
 */
@Service
public class ScenarioRecommendationServiceImpl extends ServiceImpl<ScenarioRecommendationMapper, ScenarioRecommendation>
        implements ScenarioRecommendationService {

    @Override
    public ScenarioRecommendation getByScenarioCode(String scenarioCode) {
        return getOne(new LambdaQueryWrapper<ScenarioRecommendation>()
                .eq(ScenarioRecommendation::getScenarioCode, scenarioCode));
    }
}
