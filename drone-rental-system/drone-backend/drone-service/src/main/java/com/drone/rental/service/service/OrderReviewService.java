package com.drone.rental.service.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.drone.rental.dao.entity.OrderReview;

import java.util.Map;

/**
 * 订单评价服务接口
 */
public interface OrderReviewService extends IService<OrderReview> {

    /**
     * 创建评价
     */
    OrderReview createReview(Long orderId, Long userId, Integer rating, String content,
                            String images, String tags, Integer serviceRating, Integer valueRating);

    /**
     * 检查订单是否已评价
     */
    boolean isReviewed(Long orderId);

    /**
     * 获取订单评价
     */
    OrderReview getByOrderId(Long orderId);

    /**
     * 获取无人机平均评分
     */
    Double getAvgRating(Long vehicleId);

    /**
     * 获取无人机评价统计
     */
    Map<String, Object> getReviewStats(Long vehicleId);

    /**
     * 分页查询用户评价
     */
    IPage<OrderReview> selectUserPage(IPage<OrderReview> page, Long userId);

    /**
     * 分页查询无人机评价
     */
    IPage<OrderReview> selectVehiclePage(IPage<OrderReview> page, Long vehicleId);

    /**
     * 分页查询所有评价（管理端）
     */
    IPage<OrderReview> selectPage(IPage<OrderReview> page, String keyword, Integer status);

    /**
     * 管理员回复评价
     */
    void adminReply(Long reviewId, String reply);

    /**
     * 显示/隐藏评价
     */
    void updateStatus(Long reviewId, Integer status);

    /**
     * 点赞评价
     */
    void likeReview(Long reviewId);

    /**
     * 删除评价
     */
    void deleteReview(Long reviewId);
}
