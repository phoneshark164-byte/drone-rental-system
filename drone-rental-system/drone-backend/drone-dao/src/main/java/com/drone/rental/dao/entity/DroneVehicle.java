package com.drone.rental.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 无人机实体
 */
@Data
@TableName("drone_vehicle")
public class DroneVehicle implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("drone_no")
    private String vehicleNo;

    private String brand;

    private String model;

    private String color;

    private String imageUrl;

    private Integer batteryLevel;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String locationDetail;

    private Integer status;

    /**
     * 飞行时长（小时）
     */
    @TableField("flight_hours")
    private BigDecimal flightHours;

    /**
     * 是否上架 0-下架 1-上架
     */
    private Integer isListed;

    /**
     * 充电开始时间
     */
    private LocalDateTime chargingStartTime;

    /**
     * 充电起始电量
     */
    private Integer startBatteryLevel;

    /**
     * 里程（数据库暂无此字段，设为不映射）
     */
    @TableField(exist = false)
    private BigDecimal mileage;

    private Long operatorId;

    private String areaCode;

    private LocalDateTime lastMaintenanceTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
