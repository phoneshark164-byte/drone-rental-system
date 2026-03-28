package com.drone.rental.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drone.rental.dao.entity.UserRechargeRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户充值记录Mapper
 */
@Mapper
public interface UserRechargeRecordMapper extends BaseMapper<UserRechargeRecord> {
}
