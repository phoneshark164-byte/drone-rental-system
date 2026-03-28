package com.drone.rental.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.dao.entity.DroneFeature;
import com.drone.rental.dao.mapper.DroneFeatureMapper;
import com.drone.rental.service.service.DroneFeatureService;
import org.springframework.stereotype.Service;

/**
 * 机型特征服务实现
 */
@Service
public class DroneFeatureServiceImpl extends ServiceImpl<DroneFeatureMapper, DroneFeature>
        implements DroneFeatureService {

    @Override
    public DroneFeature getByVehicleId(Long vehicleId) {
        return getOne(new LambdaQueryWrapper<DroneFeature>().eq(DroneFeature::getVehicleId, vehicleId));
    }
}
