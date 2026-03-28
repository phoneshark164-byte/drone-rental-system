package com.drone.rental.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单评价实体
 */
@Data
@TableName("order_review")
public class OrderReview implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 无人机ID
     */
    @TableField("drone_id")
    private Long vehicleId;

    /**
     * 无人机编号
     */
    @TableField("drone_no")
    private String vehicleNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 评分 1-5星
     */
    private Integer rating;

    /**
     * 评价内容
     */
    private String content;

    /**
     * 评价图片，多张图片用逗号分隔
     */
    private String images;

    /**
     * 评价标签：flight_experience(飞行体验)、aircraft_condition(机况)、ease_of_use(易用性)、cost_effectiveness(性价比)
     */
    private String tags;

    /**
     * 服务评分 1-5星
     */
    private Integer serviceRating;

    /**
     * 性价比评分 1-5星
     */
    private Integer valueRating;

    /**
     * 状态：0-隐藏 1-显示
     */
    private Integer status;

    /**
     * 管理员回复
     */
    private String adminReply;

    /**
     * 管理员回复时间
     */
    @TableField("admin_reply_time")
    private LocalDateTime adminReplyTime;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评价时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
