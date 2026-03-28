package com.drone.rental.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.drone.rental.dao.entity.DroneVehicle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 无人机Mapper
 */
@Mapper
public interface DroneVehicleMapper extends BaseMapper<DroneVehicle> {

    /**
     * 查询附近可用无人机
     */
    IPage<DroneVehicle> selectNearbyAvailable(IPage<DroneVehicle> page,
                                           @Param("latitude") Double latitude,
                                           @Param("longitude") Double longitude,
                                           @Param("radius") Double radius);
}
