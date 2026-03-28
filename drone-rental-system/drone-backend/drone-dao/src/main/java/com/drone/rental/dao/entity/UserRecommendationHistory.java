package com.drone.rental.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户推荐历史实体
 */
@Data
@TableName("user_recommendation_history")
public class UserRecommendationHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    private String scenario;

    private BigDecimal budget;

    private Integer duration;

    /**
     * 推荐无人机列表 JSON
     */
    @TableField("recommended_vehicles")
    private String recommendedVehicles;

    @TableField("algorithm_version")
    private String algorithmVersion;

    @TableField("is_accepted")
    private Integer isAccepted;

    @TableField("converted_order_id")
    private Long convertedOrderId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
