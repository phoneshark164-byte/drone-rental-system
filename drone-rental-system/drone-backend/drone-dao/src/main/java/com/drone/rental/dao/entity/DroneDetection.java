package com.drone.rental.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI损伤检测记录实体
 */
@Data
@TableName("drone_detection")
public class DroneDetection implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String detectionNo;

    private Long userId;

    private String userName;

    @TableField("drone_id")
    private Long vehicleId;

    @TableField("drone_no")
    private String vehicleNo;

    private Long orderId;

    private String imageUrl;

    @TableField("result_image_url")
    private String resultImageUrl;

    private Integer damageCount;

    @TableField("overall_severity")
    private String overallSeverity;

    private String responsibility;

    @TableField("responsibility_reason")
    private String responsibilityReason;

    private String damageDetails;

    private BigDecimal inferenceTime;

    private String modelName;

    @TableField("auto_repair_id")
    private Long autoRepairId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
