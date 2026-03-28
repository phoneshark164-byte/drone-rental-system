package com.drone.rental.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.drone.rental.dao.entity.DroneOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单Mapper
 */
@Mapper
public interface DroneOrderMapper extends BaseMapper<DroneOrder> {

    /**
     * 查询用户订单统计
     */
    Integer countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);
}
