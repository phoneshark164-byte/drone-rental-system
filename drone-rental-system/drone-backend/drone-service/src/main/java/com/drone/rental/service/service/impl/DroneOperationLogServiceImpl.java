package com.drone.rental.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.dao.entity.DroneOperationLog;
import com.drone.rental.dao.mapper.DroneOperationLogMapper;
import com.drone.rental.service.service.DroneOperationLogService;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务实现
 */
@Service
public class DroneOperationLogServiceImpl extends ServiceImpl<DroneOperationLogMapper, DroneOperationLog> implements DroneOperationLogService {
}
