package com.drone.rental.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.common.exception.BusinessException;
import com.drone.rental.dao.entity.DroneOrder;
import com.drone.rental.dao.entity.DroneVehicle;
import com.drone.rental.dao.entity.OrderReview;
import com.drone.rental.dao.entity.SysUser;
import com.drone.rental.dao.mapper.OrderReviewMapper;
import com.drone.rental.service.service.DroneOrderService;
import com.drone.rental.service.service.DroneVehicleService;
import com.drone.rental.service.service.OrderReviewService;
import com.drone.rental.service.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单评价服务实现
 */
@Service
public class OrderReviewServiceImpl extends ServiceImpl<OrderReviewMapper, OrderReview> implements OrderReviewService {

    @Autowired
    private DroneOrderService orderService;

    @Autowired
    private DroneVehicleService vehicleService;

    @Autowired
    private SysUserService userService;

    @Override
    @Transactional
    public OrderReview createReview(Long orderId, Long userId, Integer rating, String content,
                                    String images, String tags, Integer serviceRating, Integer valueRating) {
        // 验证订单
        DroneOrder order = orderService.getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权评价此订单");
        }

        // 检查是否已评价
        OrderReview existReview = getByOrderId(orderId);
        if (existReview != null) {
            throw new BusinessException("订单已评价，不可重复评价");
        }

        // 获取无人机信息
        DroneVehicle vehicle = vehicleService.getById(order.getVehicleId());
        if (vehicle == null) {
            throw new BusinessException("无人机信息不存在");
        }

        // 获取用户信息
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户信息不存在");
        }

        // 创建评价
        OrderReview review = new OrderReview();
        review.setOrderId(orderId);
        review.setOrderNo(order.getOrderNo());
        review.setVehicleId(order.getVehicleId());
        review.setVehicleNo(order.getVehicleNo());
        review.setUserId(userId);
        review.setUsername(user.getUsername());
        review.setUserAvatar(user.getAvatar());
        review.setRating(rating);
        review.setContent(content);
        review.setImages(images);
        review.setTags(tags);
        review.setServiceRating(serviceRating);
        review.setValueRating(valueRating);
        review.setStatus(1); // 默认显示
        review.setLikeCount(0);
        save(review);

        return review;
    }

    @Override
    public boolean isReviewed(Long orderId) {
        return getByOrderId(orderId) != null;
    }

    @Override
    public OrderReview getByOrderId(Long orderId) {
        return baseMapper.getByOrderId(orderId);
    }

    @Override
    public Double getAvgRating(Long vehicleId) {
        Double avgRating = baseMapper.getAvgRatingByVehicleId(vehicleId);
        return avgRating != null ? avgRating : 0.0;
    }

    @Override
    public Map<String, Object> getReviewStats(Long vehicleId) {
        Map<String, Object> stats = new HashMap<>();

        // 总评分
        Double avgRating = getAvgRating(vehicleId);
        stats.put("avgRating", avgRating != null ? BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        // 总评价数
        Integer totalCount = baseMapper.countByVehicleId(vehicleId);
        stats.put("totalCount", totalCount != null ? totalCount : 0);

        // 各星级评价数
        Map<String, Integer> ratingCounts = new HashMap<>();
        for (int i = 5; i >= 1; i--) {
            Integer count = baseMapper.countByRating(vehicleId, i);
            ratingCounts.put(String.valueOf(i), count != null ? count : 0);
        }
        stats.put("ratingCounts", ratingCounts);

        // 好评率（4星及以上为好评）
        Integer goodCount = (ratingCounts.get("4") != null ? ratingCounts.get("4") : 0) +
                          (ratingCounts.get("5") != null ? ratingCounts.get("5") : 0);
        BigDecimal goodRate = totalCount != null && totalCount > 0
                ? BigDecimal.valueOf(goodCount).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalCount), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        stats.put("goodRate", goodRate);

        return stats;
    }

    @Override
    public IPage<OrderReview> selectUserPage(IPage<OrderReview> page, Long userId) {
        LambdaQueryWrapper<OrderReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderReview::getUserId, userId);
        wrapper.orderByDesc(OrderReview::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public IPage<OrderReview> selectVehiclePage(IPage<OrderReview> page, Long vehicleId) {
        LambdaQueryWrapper<OrderReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderReview::getVehicleId, vehicleId);
        wrapper.eq(OrderReview::getStatus, 1); // 只显示已发布的评价
        wrapper.orderByDesc(OrderReview::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public IPage<OrderReview> selectPage(IPage<OrderReview> page, String keyword, Integer status) {
        LambdaQueryWrapper<OrderReview> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(OrderReview::getOrderNo, keyword)
                    .or().like(OrderReview::getVehicleNo, keyword)
                    .or().like(OrderReview::getUsername, keyword)
                    .or().like(OrderReview::getContent, keyword);
        }
        if (status != null) {
            wrapper.eq(OrderReview::getStatus, status);
        }
        wrapper.orderByDesc(OrderReview::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    @Transactional
    public void adminReply(Long reviewId, String reply) {
        OrderReview review = getById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        review.setAdminReply(reply);
        review.setAdminReplyTime(java.time.LocalDateTime.now());
        updateById(review);
    }

    @Override
    @Transactional
    public void updateStatus(Long reviewId, Integer status) {
        OrderReview review = getById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        review.setStatus(status);
        updateById(review);
    }

    @Override
    @Transactional
    public void likeReview(Long reviewId) {
        OrderReview review = getById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        review.setLikeCount(review.getLikeCount() + 1);
        updateById(review);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        OrderReview review = getById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        baseMapper.deleteById(reviewId);
    }
}
