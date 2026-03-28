package com.drone.rental.service.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.common.exception.BusinessException;
import com.drone.rental.dao.entity.DroneVehicle;
import com.drone.rental.dao.mapper.DroneVehicleMapper;
import com.drone.rental.service.service.DroneVehicleService;
import org.springframework.stereotype.Service;

/**
 * 无人机服务实现
 */
@Service
public class DroneVehicleServiceImpl extends ServiceImpl<DroneVehicleMapper, DroneVehicle> implements DroneVehicleService {

    @Override
    public IPage<DroneVehicle> selectNearbyAvailable(Long current, Long size,
                                                      Double latitude, Double longitude, Double radius) {
        // 简化实现，返回所有可用的无人机
        LambdaQueryWrapper<DroneVehicle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DroneVehicle::getStatus, 1)
                .eq(DroneVehicle::getIsListed, 1)
                .orderByDesc(DroneVehicle::getBatteryLevel);
        return page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size), wrapper);
    }

    @Override
    public DroneVehicle getByVehicleNo(String vehicleNo) {
        return getOne(new LambdaQueryWrapper<DroneVehicle>().eq(DroneVehicle::getVehicleNo, vehicleNo));
    }

    @Override
    public IPage<DroneVehicle> selectPage(IPage<DroneVehicle> page, String keyword, Integer status) {
        LambdaQueryWrapper<DroneVehicle> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(DroneVehicle::getVehicleNo, keyword)
                    .or().like(DroneVehicle::getBrand, keyword)
                    .or().like(DroneVehicle::getModel, keyword);
        }
        if (status != null) {
            wrapper.eq(DroneVehicle::getStatus, status);
        }
        wrapper.orderByDesc(DroneVehicle::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public void updateStatus(Long vehicleId, Integer status) {
        DroneVehicle vehicle = getById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException("无人机不存在");
        }
        vehicle.setStatus(status);
        updateById(vehicle);
    }

    @Override
    public void updateLocation(Long vehicleId, Double latitude, Double longitude, String locationDetail) {
        DroneVehicle vehicle = getById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException("无人机不存在");
        }
        vehicle.setLatitude(java.math.BigDecimal.valueOf(latitude));
        vehicle.setLongitude(java.math.BigDecimal.valueOf(longitude));
        vehicle.setLocationDetail(locationDetail);
        updateById(vehicle);
    }

    @Override
    public void updateBatteryLevel(Long vehicleId, Integer batteryLevel) {
        DroneVehicle vehicle = getById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException("无人机不存在");
        }
        vehicle.setBatteryLevel(batteryLevel);
        updateById(vehicle);
    }
}
