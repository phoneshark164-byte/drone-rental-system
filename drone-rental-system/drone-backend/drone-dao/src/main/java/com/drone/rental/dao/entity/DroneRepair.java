package com.drone.rental.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 故障报修实体
 */
@Data
@TableName("drone_repair")
public class DroneRepair implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String repairNo;

    @TableField("drone_id")
    private Long vehicleId;

    @TableField("drone_no")
    private String vehicleNo;

    private Long reporterId;

    private String reporterType;

    private String reporterName;

    private String reporterPhone;

    private String faultType;

    private String faultDescription;

    private String faultImages;

    private Integer status;

    private Long handlerId;

    private String handlerName;

    private LocalDateTime handleTime;

    private String handleResult;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
