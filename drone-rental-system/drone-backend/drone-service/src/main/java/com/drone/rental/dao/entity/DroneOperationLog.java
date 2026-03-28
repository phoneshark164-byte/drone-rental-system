package com.drone.rental.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
@TableName("drone_operation_log")
public class DroneOperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String operatorType;

    private Long operatorId;

    private String operatorName;

    private String operation;

    private String method;

    private String params;

    private String ip;

    private String location;

    private Integer status;

    private String errorMsg;

    private Long executeTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
