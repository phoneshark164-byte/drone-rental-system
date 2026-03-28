package com.drone.rental.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drone.rental.dao.entity.DroneRepair;
import org.apache.ibatis.annotations.Mapper;

/**
 * 故障报修Mapper
 */
@Mapper
public interface DroneRepairMapper extends BaseMapper<DroneRepair> {
}
