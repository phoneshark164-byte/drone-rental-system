package com.drone.rental.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.dao.entity.DroneRepair;
import com.drone.rental.dao.mapper.DroneRepairMapper;
import com.drone.rental.service.service.DroneRepairService;
import org.springframework.stereotype.Service;

/**
 * 故障报修服务实现
 */
@Service
public class DroneRepairServiceImpl extends ServiceImpl<DroneRepairMapper, DroneRepair> implements DroneRepairService {
}
