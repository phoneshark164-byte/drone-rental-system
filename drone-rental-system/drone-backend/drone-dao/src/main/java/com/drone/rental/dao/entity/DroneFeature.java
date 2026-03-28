package com.drone.rental.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 机型特征实体
 */
@Data
@TableName("drone_feature")
public class DroneFeature implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("vehicle_id")
    private Long vehicleId;

    /**
     * 续航时间(分钟)
     */
    private Integer flightTime;

    /**
     * 最大控制距离(米)
     */
    @TableField("max_range")
    private Integer maxRange;

    /**
     * 最大速度(km/h)
     */
    @TableField("max_speed")
    private Integer maxSpeed;

    /**
     * 相机质量
     */
    @TableField("camera_quality")
    private String cameraQuality;

    /**
     * 云台轴数
     */
    @TableField("gimbal_axis")
    private Integer gimbalAxis;

    /**
     * 是否有避障功能
     */
    @TableField("has_obstacle_avoidance")
    private Integer hasObstacleAvoidance;

    /**
     * 是否有跟随模式
     */
    @TableField("has_follow_mode")
    private Integer hasFollowMode;

    /**
     * 是否有GPS
     */
    @TableField("has_gps")
    private Integer hasGps;

    /**
     * 重量(克)
     */
    private Integer weight;

    /**
     * 是否可折叠
     */
    private Integer foldable;

    /**
     * 防水等级
     */
    @TableField("waterproof_rating")
    private String waterproofRating;

    /**
     * 载重能力(克)
     */
    @TableField("payload_capacity")
    private Integer payloadCapacity;

    /**
     * 适合技能水平
     */
    @TableField("skill_level")
    private String skillLevel;

    /**
     * 标签数组 JSON
     */
    private String tags;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
