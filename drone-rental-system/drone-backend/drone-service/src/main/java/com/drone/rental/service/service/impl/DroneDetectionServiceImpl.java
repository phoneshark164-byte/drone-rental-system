package com.drone.rental.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.dao.entity.DroneDetection;
import com.drone.rental.dao.mapper.DroneDetectionMapper;
import com.drone.rental.service.service.DroneDetectionService;
import org.springframework.stereotype.Service;

/**
 * AI损伤检测记录服务实现
 */
@Service
public class DroneDetectionServiceImpl extends ServiceImpl<DroneDetectionMapper, DroneDetection> implements DroneDetectionService {
}
