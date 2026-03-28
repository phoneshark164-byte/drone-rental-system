package com.drone.rental.service.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.drone.rental.dao.entity.DroneVehicle;

/**
 * 无人机服务接口
 */
public interface DroneVehicleService extends IService<DroneVehicle> {

    /**
     * 查询附近可用无人机
     */
    IPage<DroneVehicle> selectNearbyAvailable(Long current, Long size,
                                           Double latitude, Double longitude, Double radius);

    /**
     * 根据无人机编号查询
     */
    DroneVehicle getByVehicleNo(String vehicleNo);

    /**
     * 分页查询无人机
     */
    IPage<DroneVehicle> selectPage(IPage<DroneVehicle> page, String keyword, Integer status);

    /**
     * 更新无人机状态
     */
    void updateStatus(Long vehicleId, Integer status);

    /**
     * 更新无人机位置
     */
    void updateLocation(Long vehicleId, Double latitude, Double longitude, String locationDetail);

    /**
     * 更新无人机电量
     */
    void updateBatteryLevel(Long vehicleId, Integer batteryLevel);
}
