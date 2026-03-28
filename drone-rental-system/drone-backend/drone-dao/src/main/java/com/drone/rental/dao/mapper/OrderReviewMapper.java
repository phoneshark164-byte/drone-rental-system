package com.drone.rental.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drone.rental.dao.entity.OrderReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 订单评价Mapper
 */
@Mapper
public interface OrderReviewMapper extends BaseMapper<OrderReview> {

    /**
     * 查询无人机平均评分
     */
    @Select("SELECT AVG(rating) FROM order_review WHERE vehicle_id = #{vehicleId} AND status = 1")
    Double getAvgRatingByVehicleId(@Param("vehicleId") Long vehicleId);

    /**
     * 查询无人机评价数量
     */
    @Select("SELECT COUNT(*) FROM order_review WHERE vehicle_id = #{vehicleId} AND status = 1")
    Integer countByVehicleId(@Param("vehicleId") Long vehicleId);

    /**
     * 查询订单是否已评价
     */
    @Select("SELECT * FROM order_review WHERE order_id = #{orderId} LIMIT 1")
    OrderReview getByOrderId(@Param("orderId") Long orderId);

    /**
     * 统计各星级评价数量
     */
    @Select("SELECT COUNT(*) FROM order_review WHERE vehicle_id = #{vehicleId} AND rating = #{rating} AND status = 1")
    Integer countByRating(@Param("vehicleId") Long vehicleId, @Param("rating") Integer rating);
}
