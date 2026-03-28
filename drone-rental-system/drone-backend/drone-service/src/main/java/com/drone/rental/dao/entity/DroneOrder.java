package com.drone.rental.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
@TableName("drone_order")
public class DroneOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    @TableField("drone_id")
    private Long vehicleId;

    @TableField("drone_no")
    private String vehicleNo;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer plannedDuration;

    private Integer actualDuration;

    private BigDecimal startLatitude;

    private BigDecimal startLongitude;

    private String startLocation;

    private BigDecimal endLatitude;

    private BigDecimal endLongitude;

    private String endLocation;

    private BigDecimal amount;

    private Integer status;

    private LocalDateTime payTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
