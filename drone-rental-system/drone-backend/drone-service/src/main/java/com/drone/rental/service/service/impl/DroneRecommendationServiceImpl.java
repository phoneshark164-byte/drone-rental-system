package com.drone.rental.service.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.dao.entity.*;
import com.drone.rental.dao.mapper.DroneRecommendationRuleMapper;
import com.drone.rental.dao.mapper.ScenarioRecommendationMapper;
import com.drone.rental.service.service.DroneRecommendationService;
import com.drone.rental.service.service.DroneVehicleService;
import com.drone.rental.service.service.UserRecommendationHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能推荐服务实现
 * 采用多维度加权评分算法
 */
@Service
public class DroneRecommendationServiceImpl extends ServiceImpl<DroneRecommendationRuleMapper, DroneRecommendationRule>
        implements DroneRecommendationService {

    @Autowired
    private DroneVehicleService vehicleService;

    @Autowired
    private ScenarioRecommendationMapper scenarioRecommendationMapper;

    @Autowired
    private UserRecommendationHistoryService recommendationHistoryService;

    // 场景特征权重配置
    private static final Map<String, ScenarioWeights> SCENARIO_WEIGHTS = new HashMap<>();

    static {
        // 航拍摄影：重视摄像头质量、云台、稳定性
        SCENARIO_WEIGHTS.put("photography", new ScenarioWeights(0.35, 0.20, 0.15, 0.15, 0.15));
        // 农业植保：重视载荷、续航、防水
        SCENARIO_WEIGHTS.put("agriculture", new ScenarioWeights(0.15, 0.30, 0.25, 0.15, 0.15));
        // 设备巡检：重视续航、摄像头、避障
        SCENARIO_WEIGHTS.put("inspection", new ScenarioWeights(0.20, 0.30, 0.20, 0.15, 0.15));
        // 测绘测量：重视续航、GPS、精度
        SCENARIO_WEIGHTS.put("survey", new ScenarioWeights(0.15, 0.35, 0.25, 0.15, 0.10));
        // 竞速飞行：重视速度、重量、操控
        SCENARIO_WEIGHTS.put("racing", new ScenarioWeights(0.10, 0.15, 0.15, 0.40, 0.20));
        // 物流配送：重视载荷、续航、稳定性
        SCENARIO_WEIGHTS.put("delivery", new ScenarioWeights(0.15, 0.30, 0.25, 0.15, 0.15));
        // 新手入门：重视稳定性、避障、价格
        SCENARIO_WEIGHTS.put("learning", new ScenarioWeights(0.15, 0.15, 0.20, 0.10, 0.40));
        // 安防监控：重视续航、摄像头、夜航
        SCENARIO_WEIGHTS.put("surveillance", new ScenarioWeights(0.25, 0.30, 0.20, 0.15, 0.10));
    }

    @Override
    public List<Map<String, Object>> getAvailableScenarios() {
        LambdaQueryWrapper<ScenarioRecommendation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScenarioRecommendation::getIsActive, 1)
                .orderByAsc(ScenarioRecommendation::getId);

        return scenarioRecommendationMapper.selectList(wrapper).stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("code", s.getScenarioCode());
            map.put("name", s.getScenarioName());
            map.put("description", s.getDescription());
            map.put("icon", s.getIcon());
            map.put("priceRange", Arrays.asList(s.getPriceRangeMin(), s.getPriceRangeMax()));
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> recommendByScenario(String scenario, Integer duration) {
        ScenarioRecommendation scenarioConfig = scenarioRecommendationMapper.selectOne(
                new LambdaQueryWrapper<ScenarioRecommendation>()
                        .eq(ScenarioRecommendation::getScenarioCode, scenario)
        );

        if (scenarioConfig == null) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<DroneVehicle> vehicleWrapper = new LambdaQueryWrapper<>();
        vehicleWrapper.eq(DroneVehicle::getStatus, 1)
                .eq(DroneVehicle::getIsListed, 1);
        List<DroneVehicle> availableVehicles = vehicleService.list(vehicleWrapper);

        List<Map<String, Object>> recommendations = new ArrayList<>();
        for (DroneVehicle vehicle : availableVehicles) {
            RecommendationResult result = calculateSmartScore(vehicle, scenarioConfig, null, duration);
            Map<String, Object> rec = buildRecommendationMap(vehicle, result);
            recommendations.add(rec);
        }

        return recommendations.stream()
                .sorted((a, b) -> {
                    Object scoreA = a.get("score");
                    Object scoreB = b.get("score");
                    double valA = scoreA instanceof Number ? ((Number) scoreA).doubleValue() : 0;
                    double valB = scoreB instanceof Number ? ((Number) scoreB).doubleValue() : 0;
                    return Double.compare(valB, valA);
                })
                .limit(5)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> recommendByBudget(Double minBudget, Double maxBudget) {
        LambdaQueryWrapper<DroneVehicle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DroneVehicle::getStatus, 1)
                .eq(DroneVehicle::getIsListed, 1);

        List<DroneVehicle> vehicles = vehicleService.list(wrapper);

        List<Map<String, Object>> recommendations = new ArrayList<>();
        for (DroneVehicle vehicle : vehicles) {
            double estimatedPrice = estimatePrice(vehicle);
            if (maxBudget == null || estimatedPrice <= maxBudget) {
                if (minBudget == null || estimatedPrice >= minBudget) {
                    Map<String, Object> rec = new HashMap<>();
                    rec.put("vehicleId", vehicle.getId());
                    rec.put("vehicleNo", vehicle.getVehicleNo());
                    rec.put("brand", vehicle.getBrand());
                    rec.put("model", vehicle.getModel());
                    rec.put("imageUrl", vehicle.getImageUrl());
                    rec.put("estimatedPrice", estimatedPrice);
                    rec.put("batteryLevel", vehicle.getBatteryLevel());
                    rec.put("locationDetail", vehicle.getLocationDetail());
                    rec.put("withinBudget", estimatedPrice <= maxBudget);
                    rec.put("score", calculateBudgetScore(estimatedPrice, minBudget, maxBudget));
                    recommendations.add(rec);
                }
            }
        }

        return recommendations.stream()
                .sorted((a, b) -> {
                    Object scoreA = a.get("score");
                    Object scoreB = b.get("score");
                    double valA = scoreA instanceof Number ? ((Number) scoreA).doubleValue() : 0;
                    double valB = scoreB instanceof Number ? ((Number) scoreB).doubleValue() : 0;
                    return Double.compare(valB, valA);
                })
                .limit(10)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> smartRecommend(Long userId, String scenario, Double budget, Integer duration) {
        // 获取场景配置
        ScenarioRecommendation scenarioConfig = scenarioRecommendationMapper.selectOne(
                new LambdaQueryWrapper<ScenarioRecommendation>()
                        .eq(ScenarioRecommendation::getScenarioCode, scenario)
        );

        // 获取可用无人机
        LambdaQueryWrapper<DroneVehicle> vehicleWrapper = new LambdaQueryWrapper<>();
        vehicleWrapper.eq(DroneVehicle::getStatus, 1)
                .eq(DroneVehicle::getIsListed, 1);
        List<DroneVehicle> availableVehicles = vehicleService.list(vehicleWrapper);

        // 为每架无人机计算智能评分
        List<Map<String, Object>> recommendations = new ArrayList<>();
        for (DroneVehicle vehicle : availableVehicles) {
            RecommendationResult result = calculateSmartScore(vehicle, scenarioConfig, budget, duration);
            Map<String, Object> rec = buildRecommendationMap(vehicle, result);

            // 计算价格
            double estimatedPrice = estimatePrice(vehicle);
            rec.put("estimatedPrice", estimatedPrice);

            recommendations.add(rec);
        }

        // 预算过滤和调整
        if (budget != null) {
            recommendations = filterAndRankByBudget(recommendations, budget);
        }

        // 按综合评分排序，返回前5个
        recommendations = recommendations.stream()
                .sorted((a, b) -> {
                    Object scoreA = a.get("finalScore");
                    Object scoreB = b.get("finalScore");
                    double valA = scoreA instanceof Number ? ((Number) scoreA).doubleValue() : 0;
                    double valB = scoreB instanceof Number ? ((Number) scoreB).doubleValue() : 0;
                    return Double.compare(valB, valA);
                })
                .limit(5)
                .collect(Collectors.toList());

        // 生成推荐摘要
        Map<String, Object> response = new HashMap<>();
        response.put("scenario", scenario);
        response.put("scenarioName", scenarioConfig != null ? scenarioConfig.getScenarioName() : scenario);
        response.put("budget", budget);
        response.put("duration", duration);
        response.put("recommendations", recommendations);
        response.put("totalFound", recommendations.size());

        return response;
    }

    @Override
    public void saveRecommendationHistory(Long userId, String scenario, Double budget, Integer duration,
                                          List<Map<String, Object>> recommendations) {
        UserRecommendationHistory history = new UserRecommendationHistory();
        history.setUserId(userId);
        history.setScenario(scenario);
        history.setBudget(budget != null ? BigDecimal.valueOf(budget) : null);
        history.setDuration(duration);
        history.setRecommendedVehicles(JSONUtil.toJsonStr(recommendations));
        history.setAlgorithmVersion("v2.0-smart");
        history.setIsAccepted(0);
        recommendationHistoryService.save(history);
    }

    @Override
    public List<Map<String, Object>> getUserRecommendationHistory(Long userId) {
        LambdaQueryWrapper<UserRecommendationHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRecommendationHistory::getUserId, userId)
                .orderByDesc(UserRecommendationHistory::getCreateTime)
                .last("LIMIT 10");

        return recommendationHistoryService.list(wrapper).stream().map(h -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", h.getId());
            map.put("scenario", h.getScenario());
            map.put("budget", h.getBudget());
            map.put("duration", h.getDuration());
            map.put("recommendations", StrUtil.isNotBlank(h.getRecommendedVehicles()) ?
                    JSONUtil.toList(h.getRecommendedVehicles(), Map.class) : new ArrayList<>());
            map.put("isAccepted", h.getIsAccepted());
            map.put("createTime", h.getCreateTime());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 智能评分计算 - 多维度加权算法
     */
    private RecommendationResult calculateSmartScore(DroneVehicle vehicle, ScenarioRecommendation scenario,
                                                      Double budget, Integer duration) {
        RecommendationResult result = new RecommendationResult();

        // 获取场景权重配置
        ScenarioWeights weights = SCENARIO_WEIGHTS.getOrDefault(scenario.getScenarioCode(),
                new ScenarioWeights(0.2, 0.2, 0.2, 0.2, 0.2)); // 默认均等权重

        // 1. 品牌质量评分 (0-100)
        double brandScore = calculateBrandScore(vehicle);

        // 2. 性能特征评分 (0-100)
        double featureScore = calculateFeatureScore(vehicle, scenario);

        // 3. 电量状态评分 (0-100)
        double batteryScore = calculateBatteryScore(vehicle);

        // 4. 续航能力评分 (0-100)
        double enduranceScore = calculateEnduranceScore(vehicle, duration);

        // 5. 价格合理性评分 (0-100)
        double priceScore = calculatePriceScore(vehicle, budget);

        // 计算加权总分
        double finalScore = brandScore * weights.brand +
                featureScore * weights.feature +
                batteryScore * weights.battery +
                enduranceScore * weights.endurance +
                priceScore * weights.price;

        result.setBrandScore(brandScore);
        result.setFeatureScore(featureScore);
        result.setBatteryScore(batteryScore);
        result.setEnduranceScore(enduranceScore);
        result.setPriceScore(priceScore);
        result.setFinalScore(finalScore);

        // 生成推荐理由
        result.setReasons(generateRecommendationReasons(vehicle, scenario, result));

        return result;
    }

    /**
     * 品牌质量评分
     */
    private double calculateBrandScore(DroneVehicle vehicle) {
        String brand = vehicle.getBrand();
        String model = vehicle.getModel();
        double score = 60.0; // 基础分

        if (brand != null) {
            // 知名品牌加分
            if (brand.contains("DJI") || brand.contains("大疆")) {
                score = 95.0;
                if (model != null) {
                    if (model.contains("Mavic") || model.contains("Air")) score = 98.0;
                    else if (model.contains("Mini")) score = 92.0;
                    else if (model.contains("Avata") || model.contains("FPV")) score = 90.0;
                }
            } else if (brand.contains("Autel") || brand.contains("道通")) {
                score = 85.0;
                if (model != null && model.contains("EVO")) score = 88.0;
            } else if (brand.contains("Skydio")) {
                score = 82.0;
            } else if (brand.contains("Yuneec") || brand.contains("昊翔")) {
                score = 78.0;
            }
        }

        // 根据飞行时长调整（反映使用程度）
        if (vehicle.getFlightHours() != null) {
            double hours = vehicle.getFlightHours().doubleValue();
            if (hours > 1000) {
                score *= 0.85; // 飞行时间过长，降分
            } else if (hours < 10) {
                score *= 1.05; // 几乎全新，加分
            }
        }

        return Math.min(score, 100);
    }

    /**
     * 性能特征评分
     */
    private double calculateFeatureScore(DroneVehicle vehicle, ScenarioRecommendation scenario) {
        double score = 70.0; // 基础分
        String scenarioCode = scenario.getScenarioCode();

        // 根据场景特殊要求调整
        switch (scenarioCode) {
            case "photography":
                // 航拍重视摄像头和云台
                if (vehicle.getModel() != null) {
                    if (vehicle.getModel().contains("Pro") || vehicle.getModel().contains("哈苏")) {
                        score += 20;
                    } else if (vehicle.getModel().contains("Air") || vehicle.getModel().contains("Mavic")) {
                        score += 15;
                    }
                }
                // 检查是否有云台信息（假设3轴以上更好）
                break;

            case "agriculture":
                // 农业重视载荷和喷洒能力
                if (vehicle.getModel() != null && vehicle.getModel().contains(" Agras")) {
                    score += 25;
                }
                break;

            case "racing":
                // 竞速重视轻量和高速度
                if (vehicle.getModel() != null) {
                    if (vehicle.getModel().contains("FPV") || vehicle.getModel().contains("Avata")) {
                        score += 25;
                    }
                }
                break;

            case "inspection":
            case "survey":
                // 巡检和测绘重视精度和稳定性
                if (vehicle.getModel() != null && vehicle.getModel().contains("Matrice")) {
                    score += 20;
                }
                break;

            case "learning":
                // 新手机型加分
                if (vehicle.getModel() != null &&
                    (vehicle.getModel().contains("Mini") || vehicle.getModel().contains("Air") || vehicle.getModel().contains("御"))) {
                    score += 20;
                }
                break;
        }

        return Math.min(score, 100);
    }

    /**
     * 电量状态评分
     */
    private double calculateBatteryScore(DroneVehicle vehicle) {
        Integer batteryLevel = vehicle.getBatteryLevel();
        if (batteryLevel == null) return 70.0;

        if (batteryLevel >= 90) {
            return 100.0; // 满电
        } else if (batteryLevel >= 75) {
            return 95.0; // 电量充足
        } else if (batteryLevel >= 50) {
            return 80.0; // 电量可用
        } else if (batteryLevel >= 30) {
            return 50.0; // 电量偏低
        } else {
            return 20.0; // 电量不足
        }
    }

    /**
     * 续航能力评分
     */
    private double calculateEnduranceScore(DroneVehicle vehicle, Integer duration) {
        double score = 70.0;

        // 根据标称飞行时间估算（从型号推断）
        int estimatedFlightTime = estimateFlightTime(vehicle);

        if (duration != null) {
            // 有时长要求时，检查是否满足
            if (estimatedFlightTime >= duration) {
                score = 100.0;
                if (estimatedFlightTime >= duration * 1.5) {
                    score = 95.0; // 续航远超需求，稍微降分避免推荐过度配置
                }
            } else {
                // 不满足时长要求，按比例降分
                score = 70.0 * (estimatedFlightTime / (double) duration);
            }
        } else {
            // 无时长要求时，续航越长越好，但有上限
            if (estimatedFlightTime >= 45) {
                score = 100.0;
            } else if (estimatedFlightTime >= 35) {
                score = 90.0;
            } else if (estimatedFlightTime >= 25) {
                score = 80.0;
            } else {
                score = 60.0;
            }
        }

        return Math.max(score, 20); // 最低20分
    }

    /**
     * 价格合理性评分
     */
    private double calculatePriceScore(DroneVehicle vehicle, Double budget) {
        double estimatedPrice = estimatePrice(vehicle);

        if (budget == null) {
            // 无预算限制，按性价比给分
            if (estimatedPrice <= 50) {
                return 90.0; // 经济型，性价比高
            } else if (estimatedPrice <= 150) {
                return 95.0; // 中端，均衡选择
            } else {
                return 80.0; // 高端，性能优先
            }
        }

        // 有预算限制时计算匹配度
        double ratio = estimatedPrice / budget;

        if (ratio <= 0.5) {
            return 85.0; // 远低于预算，高性价比
        } else if (ratio <= 0.8) {
            return 100.0; // 理想价格区间
        } else if (ratio <= 1.0) {
            return 90.0; // 接近预算上限
        } else if (ratio <= 1.2) {
            return 60.0; // 略超预算
        } else {
            return 30.0; // 严重超预算
        }
    }

    /**
     * 预算过滤和重排序
     */
    private List<Map<String, Object>> filterAndRankByBudget(List<Map<String, Object>> recommendations, Double budget) {
        return recommendations.stream()
                .peek(rec -> {
                    Object finalScoreObj = rec.get("finalScore");
                    double finalScore = finalScoreObj instanceof Number ?
                            ((Number) finalScoreObj).doubleValue() : 50.0;

                    Object priceObj = rec.get("estimatedPrice");
                    double price = priceObj instanceof Number ?
                            ((Number) priceObj).doubleValue() : 100.0;

                    // 预算匹配度调整 - 更温和的降权
                    double priceRatio = price / budget;
                    if (priceRatio > 1.5) {
                        // 超预算50%以上的，适度降权
                        finalScore *= (1.0 - (priceRatio - 1.5) * 0.2);
                    } else if (priceRatio > 1.0) {
                        // 略超预算，轻微降权
                        finalScore *= 0.9;
                    } else if (priceRatio < 0.2) {
                        // 远低于预算的，可能配置不足，轻微降权
                        finalScore *= 0.95;
                    }

                    // 确保最低分
                    rec.put("finalScore", Math.max(finalScore, 25));
                })
                .filter(rec -> {
                    Object finalScoreObj = rec.get("finalScore");
                    double finalScore = finalScoreObj instanceof Number ?
                            ((Number) finalScoreObj).doubleValue() : 0;
                    return finalScore >= 25; // 降低过滤阈值，允许更多推荐
                })
                .sorted((a, b) -> {
                    Object scoreA = a.get("finalScore");
                    Object scoreB = b.get("finalScore");
                    double valA = scoreA instanceof Number ? ((Number) scoreA).doubleValue() : 0;
                    double valB = scoreB instanceof Number ? ((Number) scoreB).doubleValue() : 0;
                    return Double.compare(valB, valA);
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建推荐结果Map
     */
    private Map<String, Object> buildRecommendationMap(DroneVehicle vehicle, RecommendationResult result) {
        Map<String, Object> rec = new HashMap<>();
        rec.put("vehicleId", vehicle.getId());
        rec.put("vehicleNo", vehicle.getVehicleNo());
        rec.put("brand", vehicle.getBrand());
        rec.put("model", vehicle.getModel());
        rec.put("imageUrl", vehicle.getImageUrl());
        rec.put("batteryLevel", vehicle.getBatteryLevel());
        rec.put("locationDetail", vehicle.getLocationDetail());
        rec.put("flightHours", vehicle.getFlightHours());

        // 评分信息
        rec.put("finalScore", Math.round(result.getFinalScore()));
        rec.put("brandScore", Math.round(result.getBrandScore()));
        rec.put("featureScore", Math.round(result.getFeatureScore()));
        rec.put("batteryScore", Math.round(result.getBatteryScore()));
        rec.put("enduranceScore", Math.round(result.getEnduranceScore()));
        rec.put("priceScore", Math.round(result.getPriceScore()));

        // 推荐理由
        rec.put("reasons", result.getReasons());

        return rec;
    }

    /**
     * 生成推荐理由
     */
    private List<String> generateRecommendationReasons(DroneVehicle vehicle, ScenarioRecommendation scenario,
                                                       RecommendationResult result) {
        List<String> reasons = new ArrayList<>();

        // 品牌优势
        if (result.getBrandScore() >= 90) {
            reasons.add("知名品牌，质量可靠");
        }

        // 特征优势
        if (result.getFeatureScore() >= 85) {
            switch (scenario.getScenarioCode()) {
                case "photography":
                    reasons.add("航拍性能出色");
                    break;
                case "agriculture":
                    reasons.add("载荷能力优秀");
                    break;
                case "racing":
                    reasons.add("速度与操控俱佳");
                    break;
                case "learning":
                    reasons.add("易于上手，适合新手");
                    break;
                default:
                    reasons.add("性能配置均衡");
            }
        }

        // 电量优势
        if (result.getBatteryScore() >= 90) {
            reasons.add("电量充足，可长时间使用");
        } else if (result.getBatteryScore() >= 80) {
            reasons.add("电量状态良好");
        }

        // 续航优势
        if (result.getEnduranceScore() >= 90) {
            reasons.add("续航能力强");
        }

        // 价格优势
        if (result.getPriceScore() >= 90) {
            reasons.add("价格合理，性价比高");
        }

        // 至少保留一条理由
        if (reasons.isEmpty()) {
            reasons.add("综合表现不错，值得考虑");
        }

        return reasons;
    }

    /**
     * 预算匹配评分（旧方法兼容）
     */
    private double calculateBudgetScore(double estimatedPrice, Double minBudget, Double maxBudget) {
        if (maxBudget == null) {
            return 70.0;
        }

        double ratio = estimatedPrice / maxBudget;

        if (ratio <= 0.5) {
            return 90.0;
        } else if (ratio <= 0.8) {
            return 100.0;
        } else if (ratio <= 1.0) {
            return 80.0;
        } else {
            return 40.0;
        }
    }

    /**
     * 估算价格
     */
    private double estimatePrice(DroneVehicle vehicle) {
        String brand = vehicle.getBrand();
        String model = vehicle.getModel();

        double basePrice = 50.0;

        if (brand != null) {
            if (brand.contains("DJI") || brand.contains("大疆")) {
                basePrice = 80.0;
                if (model != null) {
                    if (model.contains("Mavic") && model.contains("Pro")) {
                        basePrice = 150.0;
                    } else if (model.contains("Mavic")) {
                        basePrice = 100.0;
                    } else if (model.contains("Mini")) {
                        basePrice = 60.0;
                    } else if (model.contains("Air") && model.contains("Pro")) {
                        basePrice = 120.0;
                    } else if (model.contains("Air")) {
                        basePrice = 80.0;
                    } else if (model.contains("Avata")) {
                        basePrice = 100.0;
                    } else if (model.contains("Agras") || model.contains("植保")) {
                        basePrice = 200.0;
                    } else if (model.contains("Matrice")) {
                        basePrice = 300.0;
                    }
                }
            } else if (brand.contains("Autel") || brand.contains("道通")) {
                basePrice = 70.0;
                if (model != null && model.contains("EVO")) {
                    basePrice = 100.0;
                }
            }
        }

        // 根据新旧程度调整价格
        if (vehicle.getFlightHours() != null) {
            double hours = vehicle.getFlightHours().doubleValue();
            if (hours > 500) {
                basePrice *= 0.7;
            } else if (hours > 200) {
                basePrice *= 0.85;
            }
        }

        return Math.round(basePrice * 10.0) / 10.0;
    }

    /**
     * 估算飞行时间（分钟）
     */
    private int estimateFlightTime(DroneVehicle vehicle) {
        String model = vehicle.getModel();

        if (model == null) return 25;

        if (model.contains("Mini")) {
            return 30;
        } else if (model.contains("Air") || model.contains("Mavic")) {
            return 34;
        } else if (model.contains("Mavic") && model.contains("3")) {
            return 46;
        } else if (model.contains("Avata")) {
            return 18;
        } else if (model.contains("Agras") || model.contains("植保")) {
            return 40;
        } else if (model.contains("Matrice")) {
            return 55;
        } else if (model.contains("FPV")) {
            return 15;
        }

        return 30; // 默认值
    }

    /**
     * 场景权重配置类
     */
    private static class ScenarioWeights {
        double brand;     // 品牌权重
        double feature;   // 特征权重
        double battery;   // 电量权重
        double endurance;  // 续航权重
        double price;     // 价格权重

        public ScenarioWeights(double brand, double feature, double battery, double endurance, double price) {
            this.brand = brand;
            this.feature = feature;
            this.battery = battery;
            this.endurance = endurance;
            this.price = price;
        }
    }

    /**
     * 推荐结果类
     */
    private static class RecommendationResult {
        private double brandScore;
        private double featureScore;
        private double batteryScore;
        private double enduranceScore;
        private double priceScore;
        private double finalScore;
        private List<String> reasons = new ArrayList<>();

        // Getters and Setters
        public double getBrandScore() { return brandScore; }
        public void setBrandScore(double brandScore) { this.brandScore = brandScore; }

        public double getFeatureScore() { return featureScore; }
        public void setFeatureScore(double featureScore) { this.featureScore = featureScore; }

        public double getBatteryScore() { return batteryScore; }
        public void setBatteryScore(double batteryScore) { this.batteryScore = batteryScore; }

        public double getEnduranceScore() { return enduranceScore; }
        public void setEnduranceScore(double enduranceScore) { this.enduranceScore = enduranceScore; }

        public double getPriceScore() { return priceScore; }
        public void setPriceScore(double priceScore) { this.priceScore = priceScore; }

        public double getFinalScore() { return finalScore; }
        public void setFinalScore(double finalScore) { this.finalScore = finalScore; }

        public List<String> getReasons() { return reasons; }
        public void setReasons(List<String> reasons) { this.reasons = reasons; }
    }
}
