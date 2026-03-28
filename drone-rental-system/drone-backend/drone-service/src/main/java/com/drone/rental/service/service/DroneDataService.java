package com.drone.rental.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.drone.rental.dao.entity.DroneVehicle;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 无人机数据查询服务 - 用于AI助手获取实时数据
 */
public interface DroneDataService extends IService<DroneVehicle> {

    /**
     * 根据无人机名称查询价格
     * @param name 无人机名称（支持模糊匹配）
     * @return 无人机信息列表
     */
    List<DroneVehicle> getByName(String name);

    /**
     * 获取所有可租赁的无人机（已上架且未被租赁）
     * @return 可租赁无人机列表
     */
    List<DroneVehicle> getAvailableDrones();

    /**
     * 根据ID获取无人机当前位置和状态
     * @param id 无人机ID
     * @return 无人机位置信息
     */
    Map<String, Object> getDroneLocation(Long id);

    /**
     * 根据编号获取无人机当前位置和状态
     * @param vehicleNo 无人机编号
     * @return 无人机位置信息
     */
    Map<String, Object> getDroneLocationByNo(String vehicleNo);

    /**
     * 获取指定价格范围内的无人机
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @return 无人机列表
     */
    List<DroneVehicle> getByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * 获取各型号的无人机数量统计
     * @return 统计信息
     */
    Map<String, Object> getModelStatistics();

    /**
     * 获取当前所有无人机的状态分布
     * @return 状态统计
     */
    Map<String, Object> getStatusStatistics();

    /**
     * 解析自然语言查询并返回结果（不带用户上下文）
     * @param question 用户问题
     * @return 查询结果的文本描述
     */
    String answerDataQuestion(String question);

    /**
     * 解析自然语言查询并返回结果（带用户上下文）
     * @param question 用户问题
     * @param userId 当前登录用户ID
     * @return 查询结果的文本描述
     */
    String answerUserDataQuestion(String question, Long userId);

    // ==================== 用户相关查询方法 ====================

    /**
     * 获取用户的报修记录
     * @param userId 用户ID
     * @return 报修记录列表
     */
    List<Map<String, Object>> getUserRepairs(Long userId);

    /**
     * 获取用户的报修统计信息
     * @param userId 用户ID
     * @return 统计信息
     */
    Map<String, Object> getUserRepairStats(Long userId);

    /**
     * 获取用户的订单列表
     * @param userId 用户ID
     * @return 订单列表
     */
    List<Map<String, Object>> getUserOrders(Long userId);

    /**
     * 获取用户的订单统计信息
     * @param userId 用户ID
     * @return 统计信息
     */
    Map<String, Object> getUserOrderStats(Long userId);

    /**
     * 获取用户的余额信息
     * @param userId 用户ID
     * @return 余额信息
     */
    Map<String, Object> getUserBalance(Long userId);

    /**
     * 获取用户的损伤检测记录
     * @param userId 用户ID
     * @return 检测记录列表
     */
    List<Map<String, Object>> getUserDetections(Long userId);

    /**
     * 获取用户的智能推荐历史
     * @param userId 用户ID
     * @return 推荐历史
     */
    List<Map<String, Object>> getUserRecommendations(Long userId);

    /**
     * 根据场景获取推荐无人机
     * @param scenario 应用场景
     * @return 推荐列表
     */
    List<Map<String, Object>> getRecommendationsByScenario(String scenario);

    /**
     * 根据预算获取推荐无人机
     * @param budget 预算金额
     * @return 推荐列表
     */
    List<Map<String, Object>> getRecommendationsByBudget(BigDecimal budget);
}
