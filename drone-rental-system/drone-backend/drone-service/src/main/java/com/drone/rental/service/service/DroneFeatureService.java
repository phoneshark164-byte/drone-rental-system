package com.drone.rental.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.drone.rental.dao.entity.DroneFeature;

/**
 * 机型特征服务接口
 */
public interface DroneFeatureService extends IService<DroneFeature> {

    /**
     * 根据无人机ID获取特征
     */
    DroneFeature getByVehicleId(Long vehicleId);
}
