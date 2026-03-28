package com.drone.rental.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 场景推荐配置实体
 */
@Data
@TableName("scenario_recommendation")
public class ScenarioRecommendation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("scenario_code")
    private String scenarioCode;

    @TableField("scenario_name")
    private String scenarioName;

    private String description;

    private String icon;

    /**
     * 优先特征列表 JSON
     */
    @TableField("priority_features")
    private String priorityFeatures;

    /**
     * 最低要求 JSON
     */
    @TableField("min_requirements")
    private String minRequirements;

    @TableField("price_range_min")
    private BigDecimal priceRangeMin;

    @TableField("price_range_max")
    private BigDecimal priceRangeMax;

    @TableField("is_active")
    private Integer isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
