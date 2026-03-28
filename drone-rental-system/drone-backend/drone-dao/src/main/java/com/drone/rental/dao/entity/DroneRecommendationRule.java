package com.drone.rental.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 推荐规则配置实体
 */
@Data
@TableName("drone_recommendation_rule")
public class DroneRecommendationRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("rule_name")
    private String ruleName;

    @TableField("rule_type")
    private String ruleType;

    private String scenario;

    @TableField("min_budget")
    private BigDecimal minBudget;

    @TableField("max_budget")
    private BigDecimal maxBudget;

    @TableField("min_duration")
    private Integer minDuration;

    @TableField("max_duration")
    private Integer maxDuration;

    private Integer priority;

    @TableField("brand_preference")
    private String brandPreference;

    /**
     * 特征权重配置 JSON
     */
    @TableField("feature_weights")
    private String featureWeights;

    @TableField("is_active")
    private Integer isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
