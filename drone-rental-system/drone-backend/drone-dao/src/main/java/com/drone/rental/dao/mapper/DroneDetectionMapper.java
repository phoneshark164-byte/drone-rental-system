package com.drone.rental.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drone.rental.dao.entity.DroneDetection;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI损伤检测记录Mapper
 */
@Mapper
public interface DroneDetectionMapper extends BaseMapper<DroneDetection> {
}
